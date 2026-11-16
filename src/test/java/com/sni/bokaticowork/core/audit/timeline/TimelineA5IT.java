package com.sni.bokaticowork.core.audit.timeline;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.support.AuditPurge;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lot A5 sur une vraie base : les trois vues (utilisateur, element, requete) reunissent les journaux
 * de sources differentes, la pagination par curseur ne perd ni ne double rien, et chaque source
 * produit un SQL valide avec chaque filtre.
 */
@Import(TimelineService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TimelineA5IT extends PostgresIntegrationTestBase {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private TimelineService timeline;
    @Autowired private org.springframework.transaction.PlatformTransactionManager transactions;

    private final Instant base = Instant.now().truncatedTo(ChronoUnit.SECONDS).minusSeconds(600);

    @BeforeEach
    void insert() {
        jdbc.update("""
                INSERT INTO audit_log (id, created_at, actor_id, actor_email, action, module, audit_status, request_id, entity_type, entity_code)
                VALUES (987650001, ?, 7, 'awa-a5@example.com', 'ISSUE', 'BILLING', 'SUCCESS', 'req-a5', 'BillingDocument', 'FAC-A5')
                """, Timestamp.from(base.plusSeconds(10)));
        jdbc.update("""
                INSERT INTO security_event (id, created_at, event_type, user_code, login, ip_address, request_id)
                VALUES (987650002, ?, 'LOGIN_SUCCESS', 'USR-A5', 'awa-a5@example.com', '1.2.3.4', 'req-a5-login')
                """, Timestamp.from(base.plusSeconds(5)));
        jdbc.update("""
                INSERT INTO email_delivery_log (id, email_number, provider, recipient_email, subject, body_type, status, attempts,
                                                related_type, related_code, created_at, updated_at)
                VALUES (987650003, 'EML-A5', 'GRAPH', 'client-a5@example.com', 'Votre facture', 'HTML', 'SENT', 1,
                        'BILLING_DOCUMENT', 'FAC-A5', ?, ?)
                """, Timestamp.from(base.plusSeconds(20)), Timestamp.from(base.plusSeconds(20)));
        jdbc.update("""
                INSERT INTO app_error (created_at, request_id, http_method, path, status, exception_class, user_email)
                VALUES (?, 'req-a5', 'POST', '/sni/api/v1/billing/documents/FAC-A5/issue', 500, 'java.lang.IllegalStateException', 'awa-a5@example.com')
                """, Timestamp.from(base.plusSeconds(11)));
        jdbc.update("""
                INSERT INTO request_log (created_at, request_id, http_method, path, route, status, duration_ms, user_email, reason)
                VALUES (?, 'req-a5', 'POST', '/sni/api/v1/billing/documents/FAC-A5/issue', '/sni/api/v1/billing/documents/{n}/issue', 500, 40, 'awa-a5@example.com', 'ERROR')
                """, Timestamp.from(base.plusSeconds(11)));
    }

    @AfterEach
    void clean() {
        AuditPurge.delete(jdbc, transactions, "DELETE FROM audit_log WHERE id = 987650001");
        AuditPurge.delete(jdbc, transactions, "DELETE FROM security_event WHERE id = 987650002");
        jdbc.update("DELETE FROM email_delivery_log WHERE id = 987650003");
        jdbc.update("DELETE FROM app_error WHERE request_id = 'req-a5'");
        jdbc.update("DELETE FROM request_log WHERE request_id = 'req-a5'");
    }

    private TimelineService.Filter filter(String user, String type, String code, String requestId) {
        return new TimelineService.Filter(base.minusSeconds(60), base.plusSeconds(60), user, type, code, requestId, Set.of());
    }

    @Test
    void anElementGathersItsActionsAndItsEmails() {
        List<TimelineService.Entry> items = timeline.read(filter(null, "BillingDocument", "fac-a5", null), null, 50).items();
        assertThat(items).extracting(TimelineService.Entry::source).containsExactly("EMAIL", "ACTION");
    }

    @Test
    void aUserGathersActionsLoginsErrorsAndRequests() {
        List<TimelineService.Entry> items = timeline.read(filter("AWA-A5@example.com", null, null, null), null, 50).items();
        assertThat(items).extracting(TimelineService.Entry::source)
                .containsExactlyInAnyOrder("ACTION", "SECURITY", "ERROR", "REQUEST");
        assertThat(timeline.read(filter("USR-A5", null, null, null), null, 50).items())
                .extracting(TimelineService.Entry::source).containsExactly("SECURITY");
    }

    @Test
    void aRequestGathersEverythingThatHappenedInIt() {
        assertThat(timeline.read(filter(null, null, null, "req-a5"), null, 50).items())
                .extracting(TimelineService.Entry::source).containsExactlyInAnyOrder("ACTION", "ERROR", "REQUEST");
    }

    @Test
    void theCursorNeitherLosesNorRepeatsAnything() {
        TimelineService.Filter all = filter("awa-a5@example.com", null, null, null);
        Set<String> seen = new HashSet<>();
        String cursor = null;
        int pages = 0;
        do {
            TimelineService.Page page = timeline.read(all, cursor, 1);
            page.items().forEach(item -> assertThat(seen.add(item.source() + item.refId())).isTrue());
            cursor = page.nextCursor();
            pages++;
        } while (cursor != null);
        assertThat(seen).hasSize(4);
        assertThat(pages).isEqualTo(4);
    }

    @Test
    void everySourceAnswersEveryFilter() {
        Instant to = Instant.now();
        Instant from = to.minus(1, ChronoUnit.DAYS);
        for (TimelineService.Filter f : List.of(
                new TimelineService.Filter(from, to, null, null, null, null, Set.of()),
                new TimelineService.Filter(from, to, "personne@example.com", null, null, null, Set.of()),
                new TimelineService.Filter(from, to, null, "Booking", "BKG-INCONNU", null, Set.of()),
                new TimelineService.Filter(from, to, null, "Booking", null, null, Set.of()),
                new TimelineService.Filter(from, to, null, null, null, "req-inconnue", Set.of()))) {
            TimelineService.Page page = timeline.read(f, null, 5);
            if (page.nextCursor() != null) {
                timeline.read(f, page.nextCursor(), 5);
            }
        }
        assertThat(timeline.export(filter(null, null, "FAC-A5", null), 10).rows()).isEqualTo(2);
    }

    @Test
    void unknownSourcesAndBadCursorsAreRefused() {
        assertThatThrownBy(() -> TimelineService.parseSources("ACTION,FOO")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> timeline.read(filter(null, null, null, null), "pas-un-curseur", 10)).isInstanceOf(BadRequestException.class);
        assertThat(TimelineService.normalizeType("BillingDocument")).isEqualTo("BILLING_DOCUMENT");
        assertThat(TimelineService.normalizeType("billing_document")).isEqualTo("BILLING_DOCUMENT");
    }
}
