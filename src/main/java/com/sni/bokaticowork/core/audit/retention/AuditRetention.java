package com.sni.bokaticowork.core.audit.retention;

import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Conservation des journaux du centre d'audit (lot A7) : chaque journal a sa duree (parametre du
 * domaine {@code AUDIT}, {@code 0} : sans limite), et ce qui la depasse est supprime chaque nuit par
 * lots. Decision du 14/11/2026 : actions 5 ans, securite 1 an, requetes une par une 30 jours, trafic
 * 13 mois (minutes 7 jours), erreurs 90 jours.
 */
@Slf4j
@Service
public class AuditRetention {

    static final int BATCH = 5_000;
    static final int MAX_BATCHES = 400;

    /** Un journal : parametre et duree par defaut, table, colonne de date, condition en plus, ajout seul. */
    public record Target(String name, String settingKey, long defaultDays, String table, String column, String extra,
                         boolean appendOnly) {
    }

    public static final List<Target> TARGETS = List.of(
            new Target("actions", "bokati.audit.retention.actions-days", 1_825, "audit_log", "created_at", null, true),
            new Target("security", "bokati.audit.retention.security-days", 365, "security_event", "created_at", null, true),
            new Target("requests", "bokati.audit.retention.requests-days", 30, "request_log", "created_at", null, false),
            new Target("trafficMinutes", "bokati.audit.retention.traffic-minutes-days", 7, "traffic_minute", "minute", null, false),
            new Target("trafficHours", "bokati.audit.retention.traffic-hours-days", 395, "traffic_hour", "hour", null, false),
            new Target("errors", "bokati.audit.retention.errors-days", 90, "app_error", "created_at", null, false),
            new Target("jobRuns", "bokati.audit.retention.jobs-days", 90, "scheduled_task_run", "started_at", null, false),
            new Target("alerts", "bokati.audit.retention.alerts-days", 365, "audit_alert", "opened_at", "status = 'RESOLVED'", false));

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final PlatformSettings settings;

    public AuditRetention(JdbcTemplate jdbc, PlatformTransactionManager transactionManager, PlatformSettings settings) {
        this.jdbc = jdbc;
        this.transactions = new TransactionTemplate(transactionManager);
        this.settings = settings;
    }

    public record TargetView(String name, String table, String settingKey, long days, Long rows, Instant oldest) {
    }

    /** Purge de la nuit, sur le dyno designe. */
    @Scheduled(cron = "0 30 3 * * *")
    public void purgeNightly() {
        Map<String, Long> deleted = purge();
        log.info("Centre d'audit : conservation appliquee {}", deleted);
    }

    /** Applique toutes les durees ; rend le nombre de lignes supprimees par journal. */
    public Map<String, Long> purge() {
        Map<String, Long> deleted = new LinkedHashMap<>();
        for (Target target : TARGETS) {
            long days = settings.number(target.settingKey(), target.defaultDays());
            if (days <= 0) {
                continue;
            }
            try {
                deleted.put(target.name(), purge(target, Instant.now().minus(Duration.ofDays(days))));
            } catch (RuntimeException exception) {
                log.warn("Centre d'audit : conservation de {} non appliquee : {}", target.table(), exception.getMessage());
            }
        }
        return deleted;
    }

    long purge(Target target, Instant before) {
        String table = target.table();
        String condition;
        Object[] args;
        if (target.appendOnly()) {
            // Journal chaine : on retire un debut de chaine (tout ce qui precede la premiere ligne a garder),
            // jamais une ligne au milieu, meme si les dates d'ecriture ne suivent pas tout a fait l'ordre.
            condition = "(chain_seq IS NULL AND " + target.column() + " < ?) OR chain_seq < coalesce("
                    + "(SELECT min(chain_seq) FROM " + table + " WHERE " + target.column() + " >= ?), "
                    + "(SELECT max(chain_seq) + 1 FROM " + table + "), 0)";
            args = new Object[]{Timestamp.from(before), Timestamp.from(before)};
        } else {
            condition = target.column() + " < ?" + (target.extra() == null ? "" : " AND " + target.extra());
            args = new Object[]{Timestamp.from(before)};
        }
        String sql = "DELETE FROM " + table + " WHERE ctid IN (SELECT ctid FROM " + table + " WHERE " + condition + " LIMIT " + BATCH + ")";
        long total = 0;
        for (int batch = 0; batch < MAX_BATCHES; batch++) {
            Integer removed = transactions.execute(status -> {
                if (target.appendOnly()) {
                    // Seule la purge peut supprimer dans un journal en ajout seul, et seulement dans sa transaction.
                    jdbc.execute("SET LOCAL bokati.audit_purge = 'on'");
                }
                return jdbc.update(sql, args);
            });
            total += removed == null ? 0 : removed;
            if (removed == null || removed < BATCH) {
                break;
            }
        }
        return total;
    }

    /** Duree en vigueur, nombre de lignes et plus ancienne ligne de chaque journal. */
    public List<TargetView> view() {
        return TARGETS.stream().map(target -> {
            long days = settings.number(target.settingKey(), target.defaultDays());
            try {
                Map<String, Object> stats = jdbc.queryForMap("SELECT count(*) AS n, min(" + target.column() + ") AS oldest FROM " + target.table());
                Object oldest = stats.get("oldest");
                return new TargetView(target.name(), target.table(), target.settingKey(), days, ((Number) stats.get("n")).longValue(),
                        oldest instanceof Timestamp t ? t.toInstant() : oldest instanceof java.time.OffsetDateTime o ? o.toInstant() : null);
            } catch (RuntimeException exception) {
                return new TargetView(target.name(), target.table(), target.settingKey(), days, null, null);
            }
        }).toList();
    }
}
