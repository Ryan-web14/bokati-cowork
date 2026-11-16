package com.sni.bokaticowork.core.audit.security;

import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lot A2 sur une vraie base : la migration passe la validation du schema, et un evenement de
 * securite reste enregistre quand l'operation qui l'a produit est annulee (un echec de connexion).
 */
@Import(SecurityEvents.class)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SecurityEventsA2IT extends PostgresIntegrationTestBase {

    @Autowired private SecurityEvents events;
    @Autowired private SecurityEventRepository repository;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private javax.sql.DataSource dataSource;

    @Test
    void anEventSurvivesTheRollbackOfItsOperation() {
        long before = repository.count();
        assertThatThrownBy(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
            events.record(SecurityEventType.LOGIN_FAILURE, null, null, "Awa@Example.com", "method", "PASSWORD", "reason", "BadCredentialsException");
            throw new IllegalStateException("echec de connexion");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(repository.count()).isEqualTo(before + 1);
        SecurityEvent saved = repository.findAll().stream().filter(event -> "awa@example.com".equals(event.getLogin())).findFirst().orElseThrow();
        assertThat(saved.getEventType()).isEqualTo("LOGIN_FAILURE");
        assertThat(saved.getDetailsJson()).contains("\"reason\":\"BadCredentialsException\"");
        com.sni.bokaticowork.support.AuditPurge.delete(new org.springframework.jdbc.core.JdbcTemplate(dataSource), transactions,
                "DELETE FROM security_event WHERE id = ?", saved.getId());
    }
}
