package com.sni.bokaticowork.core.audit.timeline;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Chronologie commune du centre d'audit (lot A5) : tous les journaux lus la ou ils sont, sans copie,
 * sous une seule forme, du plus recent au plus ancien, avec pagination par curseur.
 *
 * <p>Trois vues par les filtres : ce qu'a fait un utilisateur ({@code user}), ce qui est arrive a un
 * element ({@code entityCode}, et {@code entityType} au besoin), ce qui s'est passe dans une requete
 * ({@code requestId}). Une source sans la colonne filtree est ecartee de la lecture.</p>
 */
@Service
public class TimelineService {

    static final int DEFAULT_LIMIT = 50;
    static final int MAX_LIMIT = 200;
    static final Duration DEFAULT_WINDOW = Duration.ofDays(7);
    static final Duration MAX_WINDOW = Duration.ofDays(366);

    private final JdbcTemplate jdbc;

    public TimelineService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Une ligne de la chronologie, quelle que soit sa source. */
    public record Entry(Instant occurredAt, String source, String refId, String kind, String actor, String entityType,
                        String entityCode, String summary, String requestId, String ipAddress, String status) {
    }

    public record Page(List<Entry> items, String nextCursor) {
    }

    public record Filter(Instant from, Instant to, String user, String entityType, String entityCode, String requestId,
                         Set<String> sources) {
    }

    /**
     * Une source : table, colonnes ramenees a la forme commune. {@code actorCode} et les jointures
     * servent au filtre utilisateur ; une colonne {@code null} n'existe pas dans la source.
     */
    record Source(String name, String from, String time, String id, String kind, String actor, String actorCode,
                  String entityType, String entityCode, String summary, String requestId, String ip, String status) {
    }

    static final List<Source> SOURCES = List.of(
            new Source("ACTION", "audit_log a", "a.created_at", "a.id", "a.module || ':' || coalesce(a.action, '')", "a.actor_email",
                    null, "a.entity_type", "a.entity_code", "a.ressource", "a.request_id", "a.ip_address", "a.audit_status"),
            new Source("SECURITY", "security_event s", "s.created_at", "s.id", "s.event_type", "s.login", "s.user_code",
                    "'Users'", "s.user_code", "s.path", "s.request_id", "s.ip_address", null),
            new Source("ERROR", "app_error e", "e.created_at", "e.id", "coalesce(e.exception_class, e.error_code)", "e.user_email",
                    null, null, null, "e.http_method || ' ' || e.path", "e.request_id", "e.client_ip", "e.status::text"),
            new Source("REQUEST", "request_log r", "r.created_at", "r.id", "r.reason", "r.user_email", null, null, null,
                    "r.http_method || ' ' || r.path", "r.request_id", "r.client_ip", "r.status::text"),
            new Source("FISCAL", "fiscal_audit_log f", "f.created_at", "f.id", "f.action", "f.actor_code", null,
                    "f.entity_type", "f.entity_id", "left(f.new_value, 300)", null, "f.ip_address", null),
            new Source("CONTRACT", "contract_audit_event c", "c.occurred_at", "c.id", "c.event_type", "c.actor_name", null,
                    "'CONTRACT'", "c.contract_code", "coalesce(c.amendment_code || ' ', '') || coalesce(c.justification, '')", null, null, null),
            new Source("COMPLIANCE", "compliance_audit_entry k", "k.occurred_at", "k.id", "k.action", "k.actor", null,
                    "k.subject_type", "k.subject_code", "left(k.rationale, 300)", null, null, null),
            new Source("WALLET", "wallet_admin_action w", "w.created_at", "w.id", "w.action_type", "w.requested_by", null,
                    "'WALLET_ADMIN_ACTION'", "w.action_number", "w.reason", null, null, "w.status"),
            new Source("DOCUMENT_ACCESS", "document_access_log d LEFT JOIN users du ON du.id = d.user_id",
                    "(d.accessed_at AT TIME ZONE 'UTC')", "d.id", "d.action", "du.email", "du.user_id",
                    "'DOCUMENT'", "d.document_id::text", null, null, "d.ip_address", null),
            new Source("APPROVAL", "approval_event p", "p.occurred_at", "p.id", "p.event", "p.actor", null,
                    "'APPROVAL_REQUEST'", "p.request_id::text", "p.comment", null, null, null),
            new Source("SETTING", "setting_audit_logs g LEFT JOIN users gu ON gu.id = g.changed_by", "g.changed_at", "g.id",
                    "'SETTING_CHANGED'", "gu.email", "gu.user_id", "'PLATFORM_SETTING'", "g.setting_key", "g.reason", null, null, null),
            new Source("EMAIL", "email_delivery_log m", "m.created_at", "m.id", "m.status", null, null,
                    "m.related_type", "m.related_code", "m.subject || ' -> ' || m.recipient_email", null, null, "m.status"),
            new Source("WHATSAPP", "whatsapp_message h", "h.created_at", "h.id", "coalesce(h.purpose, h.kind)", "h.created_by", null,
                    null, "h.reference", "coalesce(h.template_name, '') || ' -> ' || h.to_phone", null, null, "coalesce(h.status_group, 'PENDING')"),
            new Source("OUTBOX", "outbox_event o", "o.created_at", "o.id", "o.event_type", null, null,
                    "o.aggregate_type", "o.aggregate_id", "left(o.last_error, 300)", "o.request_id", null, "o.status"),
            new Source("NOTIFICATION", "notification_message n", "n.created_at", "n.id", "n.event_type", null, null,
                    "n.aggregate_type", "n.aggregate_id", "n.channel || ' ' || coalesce(n.subject, '') || ' -> ' || coalesce(n.recipient_email, n.recipient_code, '')",
                    null, null, "n.status"),
            new Source("WEBHOOK", "webhook_delivery b", "b.created_at", "b.id", "b.event_type", null, null,
                    "b.aggregate_type", "b.aggregate_id", "left(b.last_error, 300)", null, null, "b.status"),
            new Source("PAWAPAY", "pawapay_callback q", "q.received_at", "q.id", "q.kind", null, null,
                    "'PAWAPAY'", "q.reference_id", "q.reported_status || ' ' || coalesce(q.detail, '')", null, "q.remote_address", "q.outcome"),
            new Source("JOB", "scheduled_task_run j", "j.started_at", "j.id", "j.task_name", null, null, null, null,
                    "j.reason || ' ' || j.duration_ms || ' ms' || coalesce(' ' || j.error, '')", null, "j.instance", "j.outcome"));

    public static Set<String> sourceNames() {
        return SOURCES.stream().map(Source::name).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Page read(Filter filter, String cursor, Integer limit) {
        int size = limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(limit, MAX_LIMIT));
        Instant to = filter.to() == null ? Instant.now() : filter.to();
        Instant from = filter.from() == null ? to.minus(DEFAULT_WINDOW) : filter.from();
        if (!from.isBefore(to)) {
            throw new BadRequestException("from doit preceder to");
        }
        if (Duration.between(from, to).compareTo(MAX_WINDOW) > 0) {
            throw new BadRequestException("PERIODE_TROP_LONGUE : 366 jours au plus");
        }
        Cursor position = Cursor.decode(cursor);
        String type = normalizeType(filter.entityType());

        List<String> branches = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        for (Source source : SOURCES) {
            if (filter.sources() != null && !filter.sources().isEmpty() && !filter.sources().contains(source.name())) {
                continue;
            }
            List<Object> branchArgs = new ArrayList<>();
            String where = where(source, filter, type, from, to, position, branchArgs);
            if (where == null) {
                continue;
            }
            branches.add("""
                    (SELECT %s AS occurred_at, '%s' AS source, %s::text AS ref_id, %s AS kind, %s AS actor, %s AS entity_type,
                            %s AS entity_code, %s AS summary, %s AS request_id, %s AS ip_address, %s AS status
                     FROM %s WHERE %s ORDER BY %s DESC LIMIT %d)""".formatted(
                    source.time(), source.name(), source.id(), source.kind(), text(source.actor()), text(source.entityType()),
                    text(source.entityCode()), text(source.summary()), text(source.requestId()), text(source.ip()),
                    text(source.status()), source.from(), where, source.time(), size + 1));
            args.addAll(branchArgs);
        }
        if (branches.isEmpty()) {
            return new Page(List.of(), null);
        }
        String sql = String.join("\nUNION ALL\n", branches)
                + "\nORDER BY occurred_at DESC, source DESC, ref_id DESC LIMIT " + (size + 1);
        List<Entry> rows = jdbc.query(sql, (rs, i) -> new Entry(rs.getTimestamp("occurred_at").toInstant(),
                rs.getString("source"), rs.getString("ref_id"), rs.getString("kind"), rs.getString("actor"),
                rs.getString("entity_type"), rs.getString("entity_code"), rs.getString("summary"), rs.getString("request_id"),
                rs.getString("ip_address"), rs.getString("status")), args.toArray());
        String next = null;
        if (rows.size() > size) {
            rows = rows.subList(0, size);
            Entry last = rows.getLast();
            next = new Cursor(last.occurredAt(), last.source(), last.refId()).encode();
        }
        return new Page(List.copyOf(rows), next);
    }

    public record Export(String csv, int rows, boolean truncated) {
    }

    /** Export CSV (point-virgule, UTF-8), du plus recent au plus ancien, borne a {@code maxRows} lignes. */
    public Export export(Filter filter, int maxRows) {
        StringBuilder csv = new StringBuilder("﻿");
        csv.append("date;source;nature;acteur;type_element;code_element;resume;identifiant_requete;adresse_ip;statut\r\n");
        int rows = 0;
        String cursor = null;
        boolean truncated = false;
        do {
            Page page = read(filter, cursor, MAX_LIMIT);
            for (Entry entry : page.items()) {
                if (rows >= maxRows) {
                    truncated = true;
                    break;
                }
                csv.append(String.join(";", cell(entry.occurredAt()), cell(entry.source()), cell(entry.kind()), cell(entry.actor()),
                        cell(entry.entityType()), cell(entry.entityCode()), cell(entry.summary()), cell(entry.requestId()),
                        cell(entry.ipAddress()), cell(entry.status()))).append("\r\n");
                rows++;
            }
            cursor = page.nextCursor();
        } while (cursor != null && !truncated);
        return new Export(csv.toString(), rows, truncated);
    }

    private static String cell(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).replace("\r", " ").replace("\n", " ");
        if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        if (text.contains(";") || text.contains("\"")) {
            text = "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    /** Conditions d'une source ; {@code null} si un filtre demande une colonne qu'elle n'a pas. */
    private static String where(Source source, Filter filter, String type, Instant from, Instant to, Cursor cursor, List<Object> args) {
        List<String> conditions = new ArrayList<>();
        conditions.add(source.time() + " >= ?");
        args.add(Timestamp.from(from));
        conditions.add(source.time() + " < ?");
        args.add(Timestamp.from(to));

        if (StringUtils.hasText(filter.user())) {
            if (source.actor() == null && source.actorCode() == null) {
                return null;
            }
            String user = filter.user().trim().toLowerCase(Locale.ROOT);
            List<String> any = new ArrayList<>();
            if (source.actor() != null) {
                any.add("lower(" + source.actor() + ") = ?");
                args.add(user);
            }
            if (source.actorCode() != null) {
                any.add("lower(" + source.actorCode() + ") = ?");
                args.add(user);
            }
            conditions.add("(" + String.join(" OR ", any) + ")");
        }
        if (StringUtils.hasText(filter.entityCode())) {
            if (source.entityCode() == null) {
                return null;
            }
            conditions.add("upper(" + source.entityCode() + ") = ?");
            args.add(filter.entityCode().trim().toUpperCase(Locale.ROOT));
        }
        if (type != null) {
            if (source.entityType() == null && (source.entityCode() == null || !StringUtils.hasText(filter.entityCode()))) {
                // Source sans type (WhatsApp...) : seulement quand le code de l'element est aussi donne.
                return null;
            }
            if (source.entityType() != null) {
                // Types ramenes a une meme forme : BillingDocument et BILLING_DOCUMENT se valent.
                conditions.add("upper(regexp_replace(" + source.entityType() + ", '([a-z0-9])([A-Z])', '\\1_\\2', 'g')) = ?");
                args.add(type);
            }
        }
        if (StringUtils.hasText(filter.requestId())) {
            if (source.requestId() == null) {
                return null;
            }
            conditions.add(source.requestId() + " = ?");
            args.add(filter.requestId().trim());
        }
        if (cursor != null) {
            int order = source.name().compareTo(cursor.source());
            Timestamp at = Timestamp.from(cursor.occurredAt());
            if (order < 0) {
                conditions.add(source.time() + " <= ?");
                args.add(at);
            } else if (order > 0) {
                conditions.add(source.time() + " < ?");
                args.add(at);
            } else {
                conditions.add("(" + source.time() + " < ? OR (" + source.time() + " = ? AND " + source.id() + "::text < ?))");
                args.add(at);
                args.add(at);
                args.add(cursor.refId());
            }
        }
        return String.join(" AND ", conditions);
    }

    private static String text(String expression) {
        return expression == null ? "NULL::text" : "(" + expression + ")::text";
    }

    /** {@code BillingDocument}, {@code billing_document} ou {@code BILLING_DOCUMENT} donnent {@code BILLING_DOCUMENT}. */
    static String normalizeType(String type) {
        if (!StringUtils.hasText(type)) {
            return null;
        }
        return type.trim().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
    }

    /** Position dans la chronologie : derniere ligne rendue (date, source, identifiant). */
    record Cursor(Instant occurredAt, String source, String refId) {

        String encode() {
            String raw = occurredAt.toEpochMilli() * 1000 + occurredAt.getNano() / 1000 % 1000 + "|" + source + "|" + refId;
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        }

        static Cursor decode(String value) {
            if (!StringUtils.hasText(value)) {
                return null;
            }
            try {
                String[] parts = new String(Base64.getUrlDecoder().decode(value.trim()), StandardCharsets.UTF_8).split("\\|", 3);
                long micros = Long.parseLong(parts[0]);
                Instant at = Instant.EPOCH.plus(micros, ChronoUnit.MICROS);
                if (!sourceNames().contains(parts[1])) {
                    throw new IllegalArgumentException(parts[1]);
                }
                return new Cursor(at, parts[1], parts[2]);
            } catch (RuntimeException exception) {
                throw new BadRequestException("Curseur invalide");
            }
        }
    }

    /** Sources demandees ({@code ACTION,SECURITY...}) ; toutes si vide ; une source inconnue est refusee. */
    public static Set<String> parseSources(String value) {
        if (!StringUtils.hasText(value)) {
            return Set.of();
        }
        Set<String> sources = Arrays.stream(value.split(",")).map(s -> s.trim().toUpperCase(Locale.ROOT))
                .filter(s -> !s.isEmpty()).collect(Collectors.toCollection(LinkedHashSet::new));
        for (String source : sources) {
            if (!sourceNames().contains(source)) {
                throw new BadRequestException("Source inconnue : " + source + " (attendu : " + String.join(", ", sourceNames()) + ")");
            }
        }
        return sources;
    }
}
