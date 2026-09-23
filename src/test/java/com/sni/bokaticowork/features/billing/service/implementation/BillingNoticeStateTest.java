package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.service.support.BillingReceivables;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Une facture qu'on vient de payer ne s'annonce pas comme une facture a payer.
 *
 * <p>Le meme courriel sert a l'envoyer et a la renvoyer une fois reglee. Sans distinction, le
 * client recevait « Votre facture est disponible » suivi d'un « Solde du : 0 XAF » · le document
 * etait juste, le message disait le contraire.</p>
 */
class BillingNoticeStateTest {

    private BillingDocumentResponse document(BillingDocumentStatus status, String total, String paid, String balance) {
        BigDecimal totalAmount = new BigDecimal(total);
        BigDecimal paidAmount = new BigDecimal(paid);
        BigDecimal balanceDue = new BigDecimal(balance);
        return new BillingDocumentResponse(
                "INV-1", BillingDocumentType.INVOICE, status,
                "MEMBER", "MBR-1", "Joël Bikindou", "joel@example.com", null, null, true,
                "BILLABLE_ITEM", "BIL-1", null, null, null, false,
                "Facture", null, null, "XAF",
                totalAmount, null, null, null, null, null,
                totalAmount, paidAmount, balanceDue, null,
                null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(),
                null, null, null, null, List.of(),
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null,
                null, null, null,
                BillingReceivables.receivable(BillingDocumentType.INVOICE, status),
                BillingReceivables.customerImpact(BillingDocumentType.INVOICE, status, balanceDue, totalAmount));
    }

    @Test
    @DisplayName("Rien de réglé · la facture est simplement disponible")
    void nothingPaidYet() {
        assertThat(BillingEmailServiceImpl.stateOf(document(BillingDocumentStatus.SENT, "25000", "0", "25000")))
                .isEqualTo(BillingEmailServiceImpl.NoticeState.ISSUED);
    }

    @Test
    @DisplayName("Soldée · c'est l'argent qui décide, pas le statut du document")
    void settledIsReadOnTheAmountsNotTheStatus() {
        // Le document reste SENT · son cycle de vie ne dit rien de ce qui a ete encaisse.
        assertThat(BillingEmailServiceImpl.stateOf(document(BillingDocumentStatus.SENT, "25000", "25000", "0")))
                .isEqualTo(BillingEmailServiceImpl.NoticeState.SETTLED);
        assertThat(BillingEmailServiceImpl.stateOf(document(BillingDocumentStatus.PAID, "25000", "25000", "0")))
                .isEqualTo(BillingEmailServiceImpl.NoticeState.SETTLED);
    }

    @Test
    @DisplayName("Une facture échue puis soldée n'est plus un rappel · elle est soldée")
    void anOverdueInvoiceOncePaidIsSettled() {
        assertThat(BillingEmailServiceImpl.stateOf(document(BillingDocumentStatus.OVERDUE, "25000", "25000", "0")))
                .isEqualTo(BillingEmailServiceImpl.NoticeState.SETTLED);
    }

    @Test
    @DisplayName("Règlement partiel · on accuse réception et on dit ce qui reste")
    void partialPaymentIsAcknowledged() {
        assertThat(BillingEmailServiceImpl.stateOf(document(BillingDocumentStatus.PARTIALLY_PAID, "25000", "10000", "15000")))
                .isEqualTo(BillingEmailServiceImpl.NoticeState.PARTIALLY_PAID);
    }

    @Test
    @DisplayName("Échue et impayée · c'est un rappel")
    void overdueAndUnpaidIsAReminder() {
        assertThat(BillingEmailServiceImpl.stateOf(document(BillingDocumentStatus.OVERDUE, "25000", "0", "25000")))
                .isEqualTo(BillingEmailServiceImpl.NoticeState.OVERDUE);
    }

    @Test
    @DisplayName("Une facture à zéro que personne n'a payée n'est pas « soldée »")
    void aZeroInvoiceNobodyPaidIsNotSettled() {
        assertThat(BillingEmailServiceImpl.stateOf(document(BillingDocumentStatus.SENT, "0", "0", "0")))
                .isEqualTo(BillingEmailServiceImpl.NoticeState.ISSUED);
    }
}
