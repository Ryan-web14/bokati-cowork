package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.support.subscription.SubscriptionWhatsAppNotifierProbe;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** facture_payee seulement pour une facture XAF soldee ; abonnement_actif avec la periode en cours. */
class PaymentWhatsAppNotifierTest {

    private static BillingDocumentResponse invoice(BillingDocumentType type, String currency, BigDecimal balance) {
        BillingDocumentResponse document = mock(BillingDocumentResponse.class);
        when(document.documentType()).thenReturn(type);
        when(document.currency()).thenReturn(currency);
        when(document.balanceDue()).thenReturn(balance);
        when(document.customerName()).thenReturn("Awa Ngoma");
        when(document.documentNumber()).thenReturn("FAC-2026-00012");
        when(document.paidAmount()).thenReturn(new BigDecimal("150000"));
        return document;
    }

    @Test
    void onlyASettledXafInvoiceSendsInvoicePaid() {
        assertThat(PaymentWhatsAppNotifier.settled(invoice(BillingDocumentType.INVOICE, "XAF", BigDecimal.ZERO))).isTrue();
        assertThat(PaymentWhatsAppNotifier.settled(invoice(BillingDocumentType.INVOICE, "XAF", new BigDecimal("1000")))).isFalse();
        assertThat(PaymentWhatsAppNotifier.settled(invoice(BillingDocumentType.INVOICE, "EUR", BigDecimal.ZERO))).isFalse();
        assertThat(PaymentWhatsAppNotifier.settled(invoice(BillingDocumentType.QUOTE, "XAF", BigDecimal.ZERO))).isFalse();
    }

    @Test
    void valuesAreFirstNameAmountAndNumber() {
        BillingDocumentResponse document = invoice(BillingDocumentType.INVOICE, "XAF", BigDecimal.ZERO);
        assertThat(PaymentWhatsAppNotifier.values(document, new BigDecimal("50000.00"))).isEqualTo(List.of("Awa", "50 000", "FAC-2026-00012"));
        assertThat(PaymentWhatsAppNotifier.values(document, null)).isEqualTo(List.of("Awa", "150 000", "FAC-2026-00012"));
    }

    @Test
    void subscriptionValuesAreNamePlanAndPeriod() {
        PlanVersion plan = mock(PlanVersion.class);
        when(plan.getName()).thenReturn("Résident mensuel");
        Subscription subscription = Subscription.builder().subscriptionNumber("SUB-1").planVersion(plan)
                .currentPeriodStart(LocalDate.of(2026, 12, 1)).currentPeriodEnd(LocalDate.of(2026, 12, 31)).build();
        assertThat(SubscriptionWhatsAppNotifierProbe.values(subscription, "Awa"))
                .isEqualTo(List.of("Awa", "Résident mensuel", "01/12/2026", "31/12/2026"));
    }
}
