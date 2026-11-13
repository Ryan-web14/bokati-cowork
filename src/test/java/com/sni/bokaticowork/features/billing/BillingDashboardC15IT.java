package com.sni.bokaticowork.features.billing;

import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.implementation.BillingDashboardService;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lot C15 sur le schema de production : deux devis dont un transforme en facture cinq jours plus
 * tard, une facture soldee en dix jours ; les indicateurs se recoupent avec ces pieces. La devise
 * de test (XTS) isole le jeu des autres tests.
 */
@Import(BillingDashboardService.class)
@org.springframework.test.context.TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class BillingDashboardC15IT extends PostgresIntegrationTestBase {

    @Autowired private BillingDashboardService dashboard;
    @Autowired private BillingDocumentRepository documents;
    @Autowired private EntityManager entityManager;

    @Test
    void shouldMatchTheDocumentsOfThePeriod() {
        String code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        LocalDate day = LocalDate.of(2026, 10, 5);
        save("DEV-A-" + code, BillingDocumentType.QUOTE, BillingDocumentStatus.CONVERTED, "500000", "0", day, null, null, null);
        save("DEV-B-" + code, BillingDocumentType.QUOTE, BillingDocumentStatus.SENT, "300000", "0", day, null, null, null);
        save("FAC-A-" + code, BillingDocumentType.INVOICE, BillingDocumentStatus.PAID, "500000", "0", day.plusDays(5), day.plusDays(15),
                "QUOTE", "DEV-A-" + code);
        save("FAC-B-" + code, BillingDocumentType.INVOICE, BillingDocumentStatus.ISSUED, "100000", "100000", day.plusDays(6), null, null, null);
        entityManager.flush();

        BillingDashboardService.Dashboard view = dashboard.dashboard(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), "XTS", 4);

        assertThat(view.quotes().issued()).isEqualTo(2);
        assertThat(view.quotes().converted()).isEqualTo(1);
        assertThat(view.quotes().conversionRate()).isEqualByComparingTo("50.0");
        assertThat(view.quotes().averageDaysToInvoice()).isEqualByComparingTo("5.0");
        assertThat(view.invoicing().invoices()).isEqualTo(2);
        assertThat(view.invoicing().invoiced()).isEqualByComparingTo("600000");
        assertThat(view.invoicing().outstanding()).isEqualByComparingTo("100000");
        assertThat(view.invoicing().averageDaysToPay()).isEqualByComparingTo("10.0");
        assertThat(view.topCustomers()).isNotEmpty();
        assertThat(view.forecast()).hasSize(4);
        // L'echeance de FAC-B est passee : elle compte pour la semaine en cours.
        assertThat(view.forecast().get(0).expected()).isGreaterThanOrEqualTo(new BigDecimal("100000"));
    }

    private void save(String number, BillingDocumentType type, BillingDocumentStatus status, String total, String due, LocalDate issued,
                      LocalDate paid, String sourceType, String sourceCode) {
        BigDecimal amount = new BigDecimal(total);
        BigDecimal balance = new BigDecimal(due);
        documents.save(BillingDocument.builder().documentNumber(number).documentType(type).status(status).customerType("CUSTOMER")
                .customerCode("CUS-C15").customerName("Client C15").currency("XTS").subtotalAmount(amount).discountAmount(BigDecimal.ZERO)
                .taxableAmount(amount).vatAmount(BigDecimal.ZERO).additionalCentAmount(BigDecimal.ZERO).taxAmount(BigDecimal.ZERO)
                .totalAmount(amount).paidAmount(amount.subtract(balance)).balanceDue(balance).issueDate(issued).dueDate(issued.plusDays(15))
                .issuedAt(Instant.now()).paidAt(paid == null ? null : paid.atStartOfDay(java.time.ZoneOffset.UTC).toInstant())
                .sourceType(sourceType).sourceCode(sourceCode).build());
    }
}
