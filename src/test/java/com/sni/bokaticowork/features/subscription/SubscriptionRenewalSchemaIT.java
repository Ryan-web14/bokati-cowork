package com.sni.bokaticowork.features.subscription;

import com.sni.bokaticowork.features.subscription.lifecycle.repository.SubscriptionDebitAttemptRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le renouvellement apres paiement tient sur le schema de production.
 */
@org.springframework.test.context.TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class SubscriptionRenewalSchemaIT extends PostgresIntegrationTestBase {

    @Autowired private DataSource dataSource;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private SubscriptionDebitAttemptRepository attemptRepository;

    @Test
    @DisplayName("Colonnes du renouvellement en attente, gabarit d'e-mail seme, requetes executables")
    void schemaAndQueries() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_name = 'subscription'
                  AND column_name IN ('pending_renewal_invoice', 'pending_renewal_start', 'pending_renewal_end')
                """, Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject(
                "SELECT template_name FROM notification_template WHERE template_code = 'SUBSCRIPTION_RENEWAL_PAYMENT_DUE'", String.class))
                .isEqualTo("subscription-renewal-payment-due");

        assertThat(subscriptionRepository.findWithPendingRenewal()).isEmpty();
        assertThat(subscriptionRepository.findActiveWithRenewalUnpaidSince(Instant.now())).isEmpty();
        assertThat(subscriptionRepository.findEndedPeriodsStillOpen(LocalDate.now())).isEmpty();
        assertThat(attemptRepository.existsBySubscriptionIdAndInvoiceNumber(-1L, "FAC-NONE")).isFalse();
    }
}
