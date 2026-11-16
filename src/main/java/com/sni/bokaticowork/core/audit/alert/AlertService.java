package com.sni.bokaticowork.core.audit.alert;

import com.sni.bokaticowork.core.audit.ops.OperationsService;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Alertes du centre d'audit (lot A6) : regles a seuil evaluees chaque minute, une alerte en cours au
 * plus par regle, envoyee une fois a son ouverture (application, courriel, WhatsApp), fermee seule
 * quand la mesure redescend. Un administrateur peut en accuser lecture.
 */
@Slf4j
@Service
public class AlertService {

    private final JdbcTemplate jdbc;
    private final AlertMetrics metrics;
    private final AlertDispatcher dispatcher;
    private final PlatformSettings settings;

    public AlertService(JdbcTemplate jdbc, AlertMetrics metrics, AlertDispatcher dispatcher, PlatformSettings settings) {
        this.jdbc = jdbc;
        this.metrics = metrics;
        this.dispatcher = dispatcher;
        this.settings = settings;
    }

    public record Alert(long id, String ruleCode, String severity, String status, String title, String message,
                        BigDecimal metricValue, BigDecimal threshold, Instant openedAt, Instant lastSeenAt, Instant resolvedAt,
                        Instant acknowledgedAt, String acknowledgedBy, String acknowledgeNote, String notifications) {
    }

    public record RuleView(String code, String settingKey, long threshold, boolean enabled, String severity, String title,
                           BigDecimal currentValue, String detail) {
    }

    /** Evaluation des regles, chaque minute, sur le dyno designe. */
    @Scheduled(fixedDelay = 60_000, initialDelay = 120_000)
    public void evaluate() {
        Map<AlertRule, AlertMetrics.Measure> measures = metrics.measure();
        for (AlertRule rule : AlertRule.values()) {
            AlertMetrics.Measure measure = measures.get(rule);
            if (measure == null) {
                continue;
            }
            try {
                apply(rule, measure);
            } catch (RuntimeException exception) {
                log.warn("Centre d'audit : regle {} non appliquee : {}", rule, exception.getMessage());
            }
        }
    }

    void apply(AlertRule rule, AlertMetrics.Measure measure) {
        long threshold = threshold(rule);
        boolean triggered = threshold > 0 && measure.value().compareTo(BigDecimal.valueOf(threshold)) >= 0;
        List<Long> open = jdbc.queryForList("SELECT id FROM audit_alert WHERE rule_code = ? AND status <> 'RESOLVED'", Long.class, rule.name());
        if (triggered) {
            String message = rule.message(plain(measure.value()), threshold)
                    + (StringUtils.hasText(measure.detail()) ? " : " + measure.detail() : "");
            if (!open.isEmpty()) {
                jdbc.update("UPDATE audit_alert SET last_seen_at = now(), metric_value = ?, message = ? WHERE id = ?",
                        measure.value(), cut(message, 1000), open.getFirst());
                return;
            }
            Long id = jdbc.queryForObject("""
                    INSERT INTO audit_alert (rule_code, severity, status, title, message, metric_value, threshold)
                    VALUES (?, ?, 'OPEN', ?, ?, ?, ?) ON CONFLICT DO NOTHING RETURNING id""", Long.class,
                    rule.name(), rule.severity(), rule.title(), cut(message, 1000), measure.value(), BigDecimal.valueOf(threshold));
            if (id != null) {
                String sent = dispatcher.dispatch(id, rule, message);
                jdbc.update("UPDATE audit_alert SET notifications = ? WHERE id = ?", cut(sent, 1000), id);
            }
        } else if (!open.isEmpty()) {
            jdbc.update("UPDATE audit_alert SET status = 'RESOLVED', resolved_at = now() WHERE id = ?", open.getFirst());
        }
    }

    public long threshold(AlertRule rule) {
        return settings.number(rule.settingKey(), rule.defaultThreshold());
    }

    public List<RuleView> rules() {
        Map<AlertRule, AlertMetrics.Measure> measures = metrics.measure();
        return java.util.Arrays.stream(AlertRule.values()).map(rule -> {
            AlertMetrics.Measure measure = measures.get(rule);
            long threshold = threshold(rule);
            return new RuleView(rule.name(), rule.settingKey(), threshold, threshold > 0, rule.severity(), rule.title(),
                    measure == null ? null : measure.value(), measure == null ? null : measure.detail());
        }).toList();
    }

    public Page<Alert> list(String status, String rule, Instant from, Instant to, int page, int size) {
        OperationsService.Where where = new OperationsService.Where()
                .eqUpper("status", status)
                .eqUpper("rule_code", rule)
                .from("opened_at", from)
                .to("opened_at", to);
        return OperationsService.page(jdbc, "SELECT * FROM audit_alert", where, "opened_at DESC, id DESC", page, size, (rs, i) -> alert(rs));
    }

    /** Accuse de lecture : l'alerte reste en cours jusqu'a ce que la mesure redescende. */
    public Alert acknowledge(long id, String by, String note) {
        int updated = jdbc.update("""
                UPDATE audit_alert SET status = CASE WHEN status = 'OPEN' THEN 'ACKNOWLEDGED' ELSE status END,
                       acknowledged_at = coalesce(acknowledged_at, now()), acknowledged_by = coalesce(acknowledged_by, ?),
                       acknowledge_note = coalesce(?, acknowledge_note)
                WHERE id = ?""", by, cut(note, 500), id);
        if (updated == 0) {
            throw new ResourceNotFoundException("Alerte introuvable : " + id);
        }
        return get(id);
    }

    public Alert get(long id) {
        List<Alert> found = jdbc.query("SELECT * FROM audit_alert WHERE id = ?", (rs, i) -> alert(rs), id);
        if (found.isEmpty()) {
            throw new ResourceNotFoundException("Alerte introuvable : " + id);
        }
        return found.getFirst();
    }

    /** Tableau de bord : indicateurs courants et alertes en cours. */
    public Map<String, Object> dashboard() {
        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("checkedAt", Instant.now());
        Map<String, Object> indicators = new LinkedHashMap<>();
        Map<AlertRule, AlertMetrics.Measure> measures = metrics.measure();
        measures.forEach((rule, measure) -> {
            Map<String, Object> indicator = new LinkedHashMap<>();
            indicator.put("value", measure.value());
            indicator.put("threshold", threshold(rule));
            indicator.put("detail", measure.detail());
            indicators.put(rule.name(), indicator);
        });
        dashboard.put("indicators", indicators);
        try {
            dashboard.put("trafficLastHour", jdbc.queryForMap("""
                    SELECT coalesce(sum(request_count), 0) AS requests, coalesce(sum(client_errors), 0) AS client_errors,
                           coalesce(sum(server_errors), 0) AS server_errors,
                           coalesce(round(sum(request_count) / 60.0, 1), 0) AS requests_per_minute
                    FROM traffic_minute WHERE minute > now() - interval '1 hour'"""));
            dashboard.put("lockedAccounts", jdbc.queryForObject(
                    "SELECT count(*) FROM users WHERE is_account_locked = true AND (locked_until IS NULL OR locked_until > now())", Long.class));
            dashboard.put("failedLogins24h", jdbc.queryForObject(
                    "SELECT count(*) FROM security_event WHERE event_type = 'LOGIN_FAILURE' AND created_at > now() - interval '24 hours'", Long.class));
            dashboard.put("openAlerts", jdbc.queryForObject("SELECT count(*) FROM audit_alert WHERE status <> 'RESOLVED'", Long.class));
        } catch (RuntimeException exception) {
            log.warn("Centre d'audit : tableau de bord partiel : {}", exception.getMessage());
        }
        return dashboard;
    }

    private static Alert alert(ResultSet rs) throws SQLException {
        return new Alert(rs.getLong("id"), rs.getString("rule_code"), rs.getString("severity"), rs.getString("status"),
                rs.getString("title"), rs.getString("message"), rs.getBigDecimal("metric_value"), rs.getBigDecimal("threshold"),
                instant(rs, "opened_at"), instant(rs, "last_seen_at"), instant(rs, "resolved_at"), instant(rs, "acknowledged_at"),
                rs.getString("acknowledged_by"), rs.getString("acknowledge_note"), rs.getString("notifications"));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static String cut(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
