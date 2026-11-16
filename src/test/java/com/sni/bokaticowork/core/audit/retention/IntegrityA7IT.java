package com.sni.bokaticowork.core.audit.retention;

import com.sni.bokaticowork.core.audit.change.AuditValues;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.support.AuditPurge;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Lot A7 sur une vraie base : chainage par empreinte pose a l'insertion, journal en ajout seul
 * (modification et suppression refusees hors de la purge), alteration et suppression detectees,
 * conservation appliquee par lots. La migration passe la validation du schema.
 */
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class IntegrityA7IT extends PostgresIntegrationTestBase {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactions;

    private long insertAction(long id, String action) {
        jdbc.update("INSERT INTO audit_log (id, created_at, actor_id, actor_email, action, module, audit_status) VALUES (?, now(), 1, 'a7@example.com', ?, 'TEST', 'SUCCESS')",
                id, action);
        return jdbc.queryForObject("SELECT chain_seq FROM audit_log WHERE id = ?", Long.class, id);
    }

    private List<AuditIntegrity.Break> breaksBetween(long first, long last) {
        return new AuditIntegrity(jdbc).verify("actions", "audit_log", "audit_log_hash").breaks().stream()
                .filter(b -> b.chainSeq() >= first && b.chainSeq() <= last).toList();
    }

    @Test
    void theChainDetectsAlterationAndDeletion() {
        long first = insertAction(987670001, "UN");
        long second = insertAction(987670002, "DEUX");
        long third = insertAction(987670003, "TROIS");
        assertThat(second).isEqualTo(first + 1);
        assertThat(third).isEqualTo(second + 1);
        Map<String, Object> row = jdbc.queryForMap("SELECT previous_hash, current_hash FROM audit_log WHERE id = 987670002");
        assertThat(row.get("previous_hash")).isEqualTo(jdbc.queryForObject("SELECT current_hash FROM audit_log WHERE id = 987670001", String.class));
        assertThat(breaksBetween(first, third)).isEmpty();

        assertThatThrownBy(() -> jdbc.update("UPDATE audit_log SET action = 'FALSIFIE' WHERE id = 987670002"))
                .hasMessageContaining("ajout seul");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM audit_log WHERE id = 987670002"))
                .hasMessageContaining("ajout seul");

        // Alteration faite en contournant les regles de la base : detectee.
        jdbc.execute("ALTER TABLE audit_log DISABLE TRIGGER trg_audit_log_append_only");
        try {
            jdbc.update("UPDATE audit_log SET action = 'FALSIFIE' WHERE id = 987670002");
        } finally {
            jdbc.execute("ALTER TABLE audit_log ENABLE TRIGGER trg_audit_log_append_only");
        }
        assertThat(breaksBetween(first, third)).containsExactly(new AuditIntegrity.Break(second, "CONTENT"));

        // Suppression au milieu (meme par la purge) : le lien rompu est detecte.
        AuditPurge.delete(jdbc, transactions, "DELETE FROM audit_log WHERE id = 987670002");
        assertThat(breaksBetween(first, third)).containsExactly(new AuditIntegrity.Break(third, "LINK"));
        AuditPurge.delete(jdbc, transactions, "DELETE FROM audit_log WHERE id IN (987670001, 987670003)");
    }

    @Test
    void securityEventsAreChainedToo() {
        jdbc.update("INSERT INTO security_event (id, created_at, event_type, login) VALUES (987670010, now(), 'LOGIN_FAILURE', 'a7@example.com')");
        Map<String, Object> row = jdbc.queryForMap("SELECT chain_seq, current_hash FROM security_event WHERE id = 987670010");
        assertThat(row.get("chain_seq")).isNotNull();
        assertThat((String) row.get("current_hash")).hasSize(64);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM security_event WHERE id = 987670010")).hasMessageContaining("ajout seul");
        AuditPurge.delete(jdbc, transactions, "DELETE FROM security_event WHERE id = 987670010");
    }

    @Test
    void retentionRemovesWhatIsTooOldInBatches() {
        PlatformSettings settings = mock(PlatformSettings.class);
        when(settings.number(anyString(), anyLong())).thenAnswer(invocation -> invocation.getArgument(1));
        AuditRetention retention = new AuditRetention(jdbc, transactions, settings);
        Instant old = Instant.now().minus(40, ChronoUnit.DAYS);
        jdbc.update("INSERT INTO request_log (created_at, http_method, path, route, status, duration_ms, reason) VALUES (?, 'GET', '/a7-old', '/a7', 200, 5000, 'SLOW')", Timestamp.from(old));
        jdbc.update("INSERT INTO request_log (created_at, http_method, path, route, status, duration_ms, reason) VALUES (now(), 'GET', '/a7-new', '/a7', 200, 5000, 'SLOW')");

        Map<String, Long> deleted = retention.purge();

        assertThat(deleted).containsKeys("actions", "security", "requests", "trafficMinutes", "trafficHours", "errors", "jobRuns", "alerts");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM request_log WHERE path = '/a7-old'", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM request_log WHERE path = '/a7-new'", Long.class)).isEqualTo(1);
        assertThat(retention.view()).extracting(AuditRetention.TargetView::name).contains("actions", "requests");
        jdbc.update("DELETE FROM request_log WHERE route = '/a7'");

        // Journal chaine : la purge passe par sa transaction declaree, sans erreur.
        long actions = retention.purge(AuditRetention.TARGETS.getFirst(), Instant.parse("2000-01-01T00:00:00Z"));
        assertThat(actions).isZero();
    }

    @Test
    void sensitiveQueryParametersAreMasked() {
        assertThat(AuditValues.maskQuery("page=2&token=abc123&code%20otp=999&q=awa"))
                .isEqualTo("page=2&token=***&code%20otp=***&q=awa");
        assertThat(AuditValues.maskQuery("bad=%E0%A4%A&apiKey=x")).isEqualTo("bad=%E0%A4%A&apiKey=***");
    }
}
