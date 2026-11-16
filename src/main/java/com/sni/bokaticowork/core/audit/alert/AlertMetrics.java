package com.sni.bokaticowork.core.audit.alert;

import com.sni.bokaticowork.core.audit.traffic.TrafficRecorder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Array;
import java.util.EnumMap;
import java.util.Map;

/**
 * Mesures lues pour les regles d'alerte et le tableau de bord (lot A6), sur les fenetres de chaque
 * regle. Une mesure illisible (table absente, base lente) est simplement omise.
 */
@Slf4j
@Component
public class AlertMetrics {

    /** En dessous, un taux d'erreurs ou un p95 n'a pas de sens. */
    static final long MIN_REQUESTS_FOR_RATE = 20;
    static final long MIN_REQUESTS_FOR_P95 = 50;

    private final JdbcTemplate jdbc;

    public AlertMetrics(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Valeur d'une mesure et detail lisible (par exemple les taches en echec). */
    public record Measure(BigDecimal value, String detail) {
    }

    public Map<AlertRule, Measure> measure() {
        Map<AlertRule, Measure> measures = new EnumMap<>(AlertRule.class);
        put(measures, AlertRule.ERROR_RATE, this::errorRate);
        put(measures, AlertRule.SERVER_ERRORS, () -> count("SELECT count(*) FROM app_error WHERE created_at > now() - interval '15 minutes'"));
        put(measures, AlertRule.SLOW_RESPONSES, this::p95);
        put(measures, AlertRule.FAILED_LOGINS, () -> count(
                "SELECT count(*) FROM security_event WHERE event_type = 'LOGIN_FAILURE' AND created_at > now() - interval '15 minutes'"));
        put(measures, AlertRule.LOCKED_ACCOUNTS, () -> count(
                "SELECT count(*) FROM security_event WHERE event_type = 'ACCOUNT_LOCKED' AND created_at > now() - interval '1 hour'"));
        put(measures, AlertRule.OUTBOX_DEAD, () -> count(
                "SELECT count(*) FROM outbox_event WHERE status = 'DEAD' AND updated_at > now() - interval '1 hour'"));
        put(measures, AlertRule.OUTBOX_WAITING, () -> count("""
                SELECT coalesce(EXTRACT(EPOCH FROM now() - min(created_at))::bigint / 60, 0) FROM outbox_event
                WHERE status IN ('PENDING', 'PROCESSING', 'FAILED')"""));
        put(measures, AlertRule.TASK_FAILURES, this::taskFailures);
        put(measures, AlertRule.EMAIL_FAILURES, () -> count(
                "SELECT count(*) FROM email_delivery_log WHERE status = 'FAILED' AND updated_at > now() - interval '1 hour'"));
        put(measures, AlertRule.WHATSAPP_FAILURES, () -> count("""
                SELECT count(*) FROM whatsapp_message WHERE status_group IN ('REJECTED', 'UNDELIVERABLE', 'EXPIRED')
                AND coalesce(done_at, created_at) > now() - interval '1 hour'"""));
        put(measures, AlertRule.PAWAPAY_FAILURES, () -> count(
                "SELECT count(*) FROM pawapay_callback WHERE outcome = 'FAILED' AND received_at > now() - interval '1 hour'"));
        return measures;
    }

    private Measure errorRate() {
        long[] totals = jdbc.queryForObject("""
                SELECT coalesce(sum(server_errors), 0), coalesce(sum(request_count), 0) FROM traffic_minute
                WHERE minute > now() - interval '5 minutes'""", (rs, i) -> new long[]{rs.getLong(1), rs.getLong(2)});
        if (totals == null || totals[1] < MIN_REQUESTS_FOR_RATE) {
            return new Measure(BigDecimal.ZERO, totals == null ? null : totals[1] + " requêtes");
        }
        BigDecimal rate = BigDecimal.valueOf(totals[0] * 100).divide(BigDecimal.valueOf(totals[1]), 1, RoundingMode.HALF_UP);
        return new Measure(rate, totals[0] + " erreurs sur " + totals[1] + " requêtes");
    }

    private Measure p95() {
        return jdbc.queryForObject("""
                WITH b AS (SELECT * FROM traffic_minute WHERE minute > now() - interval '15 minutes'),
                h AS (SELECT i, sum(x) AS v FROM b, unnest(histogram) WITH ORDINALITY AS u(x, i) GROUP BY i)
                SELECT (SELECT array_agg(v ORDER BY i) FROM h) AS hist, (SELECT coalesce(max(max_ms), 0) FROM b) AS mx,
                       (SELECT coalesce(sum(request_count), 0) FROM b) AS n""", (rs, i) -> {
            long requests = rs.getLong("n");
            if (requests < MIN_REQUESTS_FOR_P95) {
                return new Measure(BigDecimal.ZERO, requests + " requêtes");
            }
            long[] histogram = histogram(rs.getArray("hist"));
            return new Measure(BigDecimal.valueOf(TrafficRecorder.percentile(histogram, 0.95, rs.getLong("mx"))), requests + " requêtes");
        });
    }

    private Measure taskFailures() {
        return jdbc.queryForObject("""
                SELECT coalesce(max(consecutive_failures), 0) AS worst,
                       string_agg(task_name || ' (' || consecutive_failures || ')', ', ' ORDER BY consecutive_failures DESC)
                           FILTER (WHERE consecutive_failures > 0) AS tasks
                FROM scheduled_task_status""",
                (rs, i) -> new Measure(BigDecimal.valueOf(rs.getLong("worst")), rs.getString("tasks")));
    }

    private Measure count(String sql) {
        Long value = jdbc.queryForObject(sql, Long.class);
        return new Measure(BigDecimal.valueOf(value == null ? 0 : value), null);
    }

    private void put(Map<AlertRule, Measure> measures, AlertRule rule, java.util.function.Supplier<Measure> supplier) {
        try {
            measures.put(rule, supplier.get());
        } catch (RuntimeException exception) {
            log.warn("Centre d'audit : mesure {} illisible : {}", rule, exception.getMessage());
        }
    }

    private static long[] histogram(Array array) throws java.sql.SQLException {
        if (array == null) {
            return new long[0];
        }
        Object[] values = (Object[]) array.getArray();
        long[] histogram = new long[values.length];
        for (int i = 0; i < values.length; i++) {
            histogram[i] = values[i] == null ? 0 : ((Number) values[i]).longValue();
        }
        return histogram;
    }
}
