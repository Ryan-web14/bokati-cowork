package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.features.billing.enums.BillingAdvanceStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L'acompte suit ce qui est paye sur la facture.
 */
class BillingAdvanceStatusSyncTest {

    private static BillingAdvanceStatus status(BillingAdvanceStatus current, String advance, String paid) {
        return BillingDocumentServiceImpl.advanceStatus(current, new BigDecimal(advance), new BigDecimal(paid));
    }

    @Test
    @DisplayName("Acompte de 30 000 : rien paye, en attente ; 10 000, partiellement regle ; 30 000 ou plus, regle")
    void followsPaidAmount() {
        assertThat(status(BillingAdvanceStatus.PENDING, "30000", "0")).isEqualTo(BillingAdvanceStatus.PENDING);
        assertThat(status(BillingAdvanceStatus.PENDING, "30000", "10000")).isEqualTo(BillingAdvanceStatus.PARTIALLY_PAID);
        assertThat(status(BillingAdvanceStatus.PARTIALLY_PAID, "30000", "30000")).isEqualTo(BillingAdvanceStatus.PAID);
        assertThat(status(BillingAdvanceStatus.PENDING, "30000", "45000")).isEqualTo(BillingAdvanceStatus.PAID);
    }

    @Test
    @DisplayName("Un paiement annule fait revenir l'acompte en arriere ; un acompte annule ne bouge pas")
    void reversalAndCancellation() {
        assertThat(status(BillingAdvanceStatus.PAID, "30000", "10000")).isEqualTo(BillingAdvanceStatus.PARTIALLY_PAID);
        assertThat(status(BillingAdvanceStatus.PAID, "30000", "0")).isEqualTo(BillingAdvanceStatus.PENDING);
        assertThat(status(BillingAdvanceStatus.CANCELLED, "30000", "30000")).isEqualTo(BillingAdvanceStatus.CANCELLED);
    }
}
