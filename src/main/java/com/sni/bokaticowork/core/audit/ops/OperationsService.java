package com.sni.bokaticowork.core.audit.ops;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.health.contributor.HealthContributors;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.registry.HealthContributorRegistry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lecture des evenements applicatifs (centre d'audit, lot A4) : sante de l'application et de ses
 * services, boite de sortie, integrations, taches planifiees, erreurs serveur.
 */
@Slf4j
@Service
public class OperationsService {

    static final int MAX_PAGE_SIZE = 200;

    private final JdbcTemplate jdbc;
    private final ObjectProvider<HealthContributorRegistry> healthRegistry;

    public OperationsService(JdbcTemplate jdbc, ObjectProvider<HealthContributorRegistry> healthRegistry) {
        this.jdbc = jdbc;
        this.healthRegistry = healthRegistry;
    }

    public record TaskStatus(String taskName, Instant lastStartedAt, Instant lastFinishedAt, Long lastDurationMs, String lastOutcome,
                             String lastError, Instant lastFailureAt, long runCount, long failureCount, int consecutiveFailures,
                             long averageDurationMs, long maxDurationMs, String instance, Instant updatedAt) {
    }

    public record TaskRun(long id, String taskName, Instant startedAt, Instant finishedAt, long durationMs, String outcome,
                          String error, String reason, String instance) {
    }

    public record AppError(long id, Instant createdAt, String traceId, String requestId, String httpMethod, String path, int status,
                           String errorCode, String exceptionClass, String message, String rootCause, String userEmail,
                           String clientIp, String stackTrace) {
    }

    // ── Sante ──────────────────────────────────────────────────────────────────

    /** Etat d'ensemble : {@code status} vaut {@code DOWN}, {@code WARNING} ou {@code UP}. */
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("checkedAt", Instant.now());
        Map<String, Object> components = components();
        result.put("components", components);
        Map<String, Object> outbox = section(this::outbox);
        Map<String, Object> integrations = section(this::integrations);
        Map<String, Object> tasks = section(this::tasksSummary);
        Map<String, Object> errors = section(this::errorsSummary);
        result.put("outbox", outbox);
        result.put("integrations", integrations);
        result.put("scheduledTasks", tasks);
        result.put("errors", errors);
        boolean down = components.values().stream().anyMatch(c -> c instanceof Map<?, ?> m && "DOWN".equals(m.get("status")));
        boolean warning = number(outbox.get("dead")) > 0 || number(outbox.get("oldestWaitingSeconds")) > 900
                || number(tasks.get("failing")) > 0 || number(errors.get("lastHour")) > 0;
        result.put("status", down ? "DOWN" : warning ? "WARNING" : "UP");
        return result;
    }

    private Map<String, Object> components() {
        Map<String, Object> components = new LinkedHashMap<>();
        HealthContributorRegistry registry = healthRegistry.getIfAvailable();
        if (registry == null) {
            return components;
        }
        for (HealthContributors.Entry entry : registry) {
            collect(entry.name(), entry.contributor(), components);
        }
        return components;
    }

    private void collect(String name, HealthContributor contributor, Map<String, Object> into) {
        if (contributor instanceof HealthIndicator indicator) {
            long start = System.nanoTime();
            Map<String, Object> component = new LinkedHashMap<>();
            try {
                Health health = indicator.health(false);
                component.put("status", health.getStatus().getCode());
            } catch (RuntimeException exception) {
                component.put("status", "DOWN");
                component.put("error", exception.getClass().getSimpleName());
            }
            component.put("durationMs", (System.nanoTime() - start) / 1_000_000);
            into.put(name, component);
        } else if (contributor instanceof HealthContributors composite) {
            for (HealthContributors.Entry child : composite) {
                collect(name + "." + child.name(), child.contributor(), into);
            }
        }
    }

    private Map<String, Object> outbox() {
        Map<String, Object> outbox = new LinkedHashMap<>();
        jdbc.query("SELECT status, count(*) FROM outbox_event GROUP BY status", rs -> {
            outbox.put(rs.getString(1).toLowerCase(java.util.Locale.ROOT), rs.getLong(2));
        });
        outbox.putIfAbsent("dead", 0L);
        Long oldest = jdbc.queryForObject("""
                SELECT EXTRACT(EPOCH FROM now() - min(created_at))::bigint FROM outbox_event
                WHERE status IN ('PENDING', 'PROCESSING', 'FAILED')
                """, Long.class);
        outbox.put("oldestWaitingSeconds", oldest == null ? 0L : oldest);
        return outbox;
    }

    private Map<String, Object> integrations() {
        Map<String, Object> integrations = new LinkedHashMap<>();
        integrations.put("periodHours", 24);
        integrations.put("email", counts("SELECT status, count(*) FROM email_delivery_log WHERE created_at > now() - interval '24 hours' GROUP BY status"));
        integrations.put("whatsapp", counts("SELECT coalesce(status_group, 'PENDING'), count(*) FROM whatsapp_message WHERE created_at > now() - interval '24 hours' GROUP BY 1"));
        integrations.put("pawapayCallbacks", counts("SELECT coalesce(outcome, 'NONE'), count(*) FROM pawapay_callback WHERE received_at > now() - interval '24 hours' GROUP BY 1"));
        return integrations;
    }

    private Map<String, Object> tasksSummary() {
        Map<String, Object> tasks = new LinkedHashMap<>();
        tasks.put("tracked", jdbc.queryForObject("SELECT count(*) FROM scheduled_task_status", Long.class));
        tasks.put("failing", jdbc.queryForObject("SELECT count(*) FROM scheduled_task_status WHERE consecutive_failures > 0", Long.class));
        tasks.put("failingTasks", jdbc.queryForList(
                "SELECT task_name FROM scheduled_task_status WHERE consecutive_failures > 0 ORDER BY task_name", String.class));
        return tasks;
    }

    private Map<String, Object> errorsSummary() {
        Map<String, Object> errors = new LinkedHashMap<>();
        errors.put("lastHour", jdbc.queryForObject("SELECT count(*) FROM app_error WHERE created_at > now() - interval '1 hour'", Long.class));
        errors.put("last24Hours", jdbc.queryForObject("SELECT count(*) FROM app_error WHERE created_at > now() - interval '24 hours'", Long.class));
        Timestamp lastAt = jdbc.queryForObject("SELECT max(created_at) FROM app_error", Timestamp.class);
        errors.put("lastAt", lastAt == null ? null : lastAt.toInstant());
        return errors;
    }

    private Map<String, Long> counts(String sql) {
        Map<String, Long> counts = new LinkedHashMap<>();
        jdbc.query(sql, rs -> {
            counts.put(rs.getString(1), rs.getLong(2));
        });
        return counts;
    }

    private Map<String, Object> section(java.util.function.Supplier<Map<String, Object>> supplier) {
        try {
            return supplier.get();
        } catch (RuntimeException exception) {
            log.warn("Centre d'audit : sante partielle : {}", exception.getMessage());
            Map<String, Object> unavailable = new LinkedHashMap<>();
            unavailable.put("unavailable", true);
            return unavailable;
        }
    }

    private static long number(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }

    // ── Taches planifiees ──────────────────────────────────────────────────────

    public List<TaskStatus> tasks() {
        return jdbc.query("""
                SELECT * FROM scheduled_task_status
                ORDER BY (consecutive_failures > 0) DESC, task_name
                """, (rs, i) -> new TaskStatus(
                rs.getString("task_name"), instant(rs, "last_started_at"), instant(rs, "last_finished_at"),
                (Long) rs.getObject("last_duration_ms"), rs.getString("last_outcome"), rs.getString("last_error"),
                instant(rs, "last_failure_at"), rs.getLong("run_count"), rs.getLong("failure_count"),
                rs.getInt("consecutive_failures"),
                rs.getLong("run_count") == 0 ? 0 : rs.getLong("total_duration_ms") / rs.getLong("run_count"),
                rs.getLong("max_duration_ms"), rs.getString("instance"), instant(rs, "updated_at")));
    }

    public Page<TaskRun> runs(String task, String outcome, Instant from, Instant to, int page, int size) {
        Where where = new Where()
                .eq("task_name", task)
                .eqUpper("outcome", outcome)
                .from("started_at", from)
                .to("started_at", to);
        return page("SELECT * FROM scheduled_task_run", where, "started_at DESC, id DESC", page, size,
                (rs, i) -> new TaskRun(rs.getLong("id"), rs.getString("task_name"), instant(rs, "started_at"),
                        instant(rs, "finished_at"), rs.getLong("duration_ms"), rs.getString("outcome"), rs.getString("error"),
                        rs.getString("reason"), rs.getString("instance")));
    }

    // ── Erreurs serveur ────────────────────────────────────────────────────────

    public Page<AppError> errors(Instant from, Instant to, String path, String exception, String requestId, int page, int size) {
        Where where = new Where()
                .from("created_at", from)
                .to("created_at", to)
                .like("path", path)
                .like("exception_class", exception)
                .eq("request_id", requestId);
        return page("SELECT * FROM app_error", where, "created_at DESC, id DESC", page, size, (rs, i) -> error(rs, false));
    }

    public AppError error(long id) {
        List<AppError> found = jdbc.query("SELECT * FROM app_error WHERE id = ?", (rs, i) -> error(rs, true), id);
        if (found.isEmpty()) {
            throw new ResourceNotFoundException("Erreur introuvable : " + id);
        }
        return found.getFirst();
    }

    private AppError error(ResultSet rs, boolean withStack) throws SQLException {
        return new AppError(rs.getLong("id"), instant(rs, "created_at"), rs.getString("trace_id"), rs.getString("request_id"),
                rs.getString("http_method"), rs.getString("path"), rs.getInt("status"), rs.getString("error_code"),
                rs.getString("exception_class"), rs.getString("message"), rs.getString("root_cause"), rs.getString("user_email"),
                rs.getString("client_ip"), withStack ? rs.getString("stack_trace") : null);
    }

    // ── Outils ─────────────────────────────────────────────────────────────────

    private <T> Page<T> page(String select, Where where, String order, int page, int size, org.springframework.jdbc.core.RowMapper<T> mapper) {
        if (page < 0 || size < 1) {
            throw new BadRequestException("page doit etre positive et size au moins 1");
        }
        int limit = Math.min(size, MAX_PAGE_SIZE);
        Long total = jdbc.queryForObject("SELECT count(*) FROM (" + select + where.sql() + ") filtered", Long.class, where.args());
        List<Object> args = new ArrayList<>(List.of(where.args()));
        args.add(limit);
        args.add((long) page * limit);
        List<T> content = jdbc.query(select + where.sql() + " ORDER BY " + order + " LIMIT ? OFFSET ?", mapper, args.toArray());
        return new PageImpl<>(content, PageRequest.of(page, limit), total == null ? 0 : total);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    /** Clause WHERE construite avec des parametres lies (aucune valeur saisie dans le SQL). */
    static final class Where {
        private final StringBuilder sql = new StringBuilder();
        private final List<Object> args = new ArrayList<>();

        private Where add(String condition, Object arg) {
            sql.append(sql.length() == 0 ? " WHERE " : " AND ").append(condition);
            args.add(arg);
            return this;
        }

        Where eq(String column, String value) {
            return StringUtils.hasText(value) ? add(column + " = ?", value.trim()) : this;
        }

        Where eqUpper(String column, String value) {
            return StringUtils.hasText(value) ? add(column + " = ?", value.trim().toUpperCase(java.util.Locale.ROOT)) : this;
        }

        Where like(String column, String value) {
            return StringUtils.hasText(value) ? add("lower(" + column + ") LIKE ?", "%" + value.trim().toLowerCase(java.util.Locale.ROOT) + "%") : this;
        }

        Where from(String column, Instant value) {
            return value == null ? this : add(column + " >= ?", Timestamp.from(value));
        }

        Where to(String column, Instant value) {
            return value == null ? this : add(column + " <= ?", Timestamp.from(value));
        }

        String sql() {
            return sql.toString();
        }

        Object[] args() {
            return args.toArray();
        }
    }
}
