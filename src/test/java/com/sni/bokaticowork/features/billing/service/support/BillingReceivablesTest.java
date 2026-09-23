package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ce qu'un client doit, et ce qui n'en est pas.
 *
 * <p>Deux erreurs opposees vivaient ensemble : une facture en brouillon comptait comme due, et un
 * avoir aussi. Le client voyait un solde qu'on ne lui avait jamais reclame, et on lui reclamait
 * ce qu'on lui devait.</p>
 */
class BillingReceivablesTest {

    @Test
    @DisplayName("Une facture en brouillon n'est rien · elle n'a jamais ete presentee au client")
    void aDraftInvoiceIsNotADebt() {
        assertThat(BillingReceivables.issued(BillingDocumentType.INVOICE, BillingDocumentStatus.DRAFT)).isFalse();
        assertThat(BillingReceivables.receivable(BillingDocumentType.INVOICE, BillingDocumentStatus.DRAFT)).isFalse();
        assertThat(BillingReceivables.customerImpact(BillingDocumentType.INVOICE, BillingDocumentStatus.DRAFT,
                new BigDecimal("50000"), new BigDecimal("50000"))).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @ParameterizedTest
    @EnumSource(value = BillingDocumentStatus.class,
            names = {"ISSUED", "VALIDATED", "SENT", "VIEWED", "PARTIALLY_PAID", "OVERDUE"})
    @DisplayName("Une facture emise, scellee ou echue est une creance")
    void anIssuedInvoiceIsAReceivable(BillingDocumentStatus status) {
        assertThat(BillingReceivables.receivable(BillingDocumentType.INVOICE, status)).isTrue();
        assertThat(BillingReceivables.customerImpact(BillingDocumentType.INVOICE, status,
                new BigDecimal("50000"), new BigDecimal("50000"))).isEqualByComparingTo(new BigDecimal("50000"));
    }

    @Test
    @DisplayName("Une facture scellee compte · c'est justement celle qui est definitive")
    void aSealedInvoiceIsNeverForgotten() {
        assertThat(BillingReceivables.receivable(BillingDocumentType.INVOICE, BillingDocumentStatus.VALIDATED)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = BillingDocumentStatus.class, names = {"PAID", "REFUNDED", "WRITTEN_OFF"})
    @DisplayName("Une facture reglee, remboursee ou passee en perte ne se reclame plus")
    void aSettledInvoiceIsNoLongerClaimed(BillingDocumentStatus status) {
        assertThat(BillingReceivables.issued(BillingDocumentType.INVOICE, status)).isTrue();
        assertThat(BillingReceivables.receivable(BillingDocumentType.INVOICE, status)).isFalse();
        assertThat(BillingReceivables.customerImpact(BillingDocumentType.INVOICE, status,
                new BigDecimal("50000"), new BigDecimal("50000"))).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @ParameterizedTest
    @EnumSource(value = BillingDocumentStatus.class, names = {"CANCELLED", "VOIDED", "REJECTED", "EXPIRED", "CONVERTED"})
    @DisplayName("Un document abandonne ne compte ni comme facture, ni comme dette")
    void anAbandonedDocumentCountsForNothing(BillingDocumentStatus status) {
        assertThat(BillingReceivables.issued(BillingDocumentType.INVOICE, status)).isFalse();
        assertThat(BillingReceivables.receivable(BillingDocumentType.INVOICE, status)).isFalse();
    }

    @Test
    @DisplayName("Un avoir n'est jamais une dette · il compte en negatif quand il est disponible")
    void aCreditNoteIsNeverADebt() {
        assertThat(BillingReceivables.receivable(BillingDocumentType.CREDIT_NOTE, BillingDocumentStatus.VALIDATED)).isFalse();
        assertThat(BillingReceivables.creditAvailable(BillingDocumentType.CREDIT_NOTE, BillingDocumentStatus.VALIDATED)).isTrue();
        assertThat(BillingReceivables.customerImpact(BillingDocumentType.CREDIT_NOTE, BillingDocumentStatus.VALIDATED,
                new BigDecimal("20000"), new BigDecimal("20000"))).isEqualByComparingTo(new BigDecimal("-20000"));
    }

    @Test
    @DisplayName("Un avoir consomme ne compte plus · sur un avoir, ISSUED veut dire consomme")
    void aConsumedCreditNoteCountsForNothing() {
        assertThat(BillingReceivables.creditAvailable(BillingDocumentType.CREDIT_NOTE, BillingDocumentStatus.ISSUED)).isFalse();
        assertThat(BillingReceivables.customerImpact(BillingDocumentType.CREDIT_NOTE, BillingDocumentStatus.ISSUED,
                new BigDecimal("20000"), new BigDecimal("20000"))).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Un avoir en brouillon n'est pas encore un credit · il n'est pas scelle")
    void aDraftCreditNoteIsNotYetACredit() {
        assertThat(BillingReceivables.creditAvailable(BillingDocumentType.CREDIT_NOTE, BillingDocumentStatus.DRAFT)).isFalse();
    }

    @Test
    @DisplayName("Un devis n'est pas une creance · rien n'a ete facture")
    void aQuoteIsNotAReceivable() {
        assertThat(BillingReceivables.receivable(BillingDocumentType.QUOTE, BillingDocumentStatus.ACCEPTED)).isFalse();
        assertThat(BillingReceivables.customerImpact(BillingDocumentType.QUOTE, BillingDocumentStatus.ACCEPTED,
                new BigDecimal("50000"), new BigDecimal("50000"))).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Une proforma emise reste une creance · c'est le comportement en place")
    void aProformaStaysAReceivable() {
        assertThat(BillingReceivables.receivable(BillingDocumentType.PROFORMA_INVOICE, BillingDocumentStatus.SENT)).isTrue();
    }

    @Test
    @DisplayName("Additionner customerImpact sur un releve donne le solde juste")
    void summingCustomerImpactGivesTheRightBalance() {
        BigDecimal total = BigDecimal.ZERO
                // Facture emise · 50 000 dus
                .add(BillingReceivables.customerImpact(BillingDocumentType.INVOICE, BillingDocumentStatus.SENT,
                        new BigDecimal("50000"), new BigDecimal("50000")))
                // Brouillon · rien
                .add(BillingReceivables.customerImpact(BillingDocumentType.INVOICE, BillingDocumentStatus.DRAFT,
                        new BigDecimal("30000"), new BigDecimal("30000")))
                // Avoir disponible · 20 000 en notre defaveur
                .add(BillingReceivables.customerImpact(BillingDocumentType.CREDIT_NOTE, BillingDocumentStatus.VALIDATED,
                        new BigDecimal("20000"), new BigDecimal("20000")))
                // Facture deja reglee · rien
                .add(BillingReceivables.customerImpact(BillingDocumentType.INVOICE, BillingDocumentStatus.PAID,
                        BigDecimal.ZERO, new BigDecimal("15000")));

        assertThat(total).isEqualByComparingTo(new BigDecimal("30000"));
    }

    @Test
    @DisplayName("Un statut absent ne fait rien compter · on ne devine pas")
    void aMissingStatusCountsForNothing() {
        assertThat(BillingReceivables.issued(BillingDocumentType.INVOICE, null)).isFalse();
        assertThat(BillingReceivables.receivable(null, BillingDocumentStatus.SENT)).isFalse();
    }
}
