package com.sni.bokaticowork.features.subscription.subscription.service.support.subscription;

import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentIntentFromBillingDocumentRequest;
import com.sni.bokaticowork.features.payment.dto.request.WalletPaymentRequest;
import com.sni.bokaticowork.features.payment.dto.response.PaymentIntentResponse;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.model.WalletHold;
import com.sni.bokaticowork.features.payment.repository.WalletHoldRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentService;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletHoldService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le depot a l'annulation · rendu, puis employe a regler les impayes, la plus ancienne d'abord.
 */
class SubscriptionDepositSettlementTest {

    private final WalletHoldRepository holds = mock(WalletHoldRepository.class);
    private final WalletHoldService holdService = mock(WalletHoldService.class);
    private final BillingDocumentRepository documents = mock(BillingDocumentRepository.class);
    private final PaymentService payments = mock(PaymentService.class);
    private final SubscriptionDepositSettlement settlement = new SubscriptionDepositSettlement(holds, holdService, documents, payments);

    private static BillingDocument invoice(String number, String due) {
        BillingDocument document = new BillingDocument();
        document.setDocumentNumber(number);
        document.setBalanceDue(new BigDecimal(due));
        document.setCurrency("XAF");
        return document;
    }

    @Test
    @DisplayName("100 000 de depot, impayes de 60 000 puis 70 000 : la premiere reglee, 40 000 sur la seconde, rien de rendu")
    void paysOldestFirst() {
        WalletHold hold = WalletHold.builder().holdNumber("WHL-1").amount(new BigDecimal("100000")).currency("XAF")
                .wallet(WalletAccount.builder().walletNumber("WLT-1").build()).build();
        when(holds.findAllByStatusAndSourceTypeAndSourceCode("ACTIVE", "SUBSCRIPTION", "SUB-1")).thenReturn(List.of(hold));
        when(documents.findRecoverableDocuments("MEMBER", "MBR-1")).thenReturn(List.of(invoice("INV-1", "60000"), invoice("INV-2", "70000")));
        PaymentIntentResponse intent = mock(PaymentIntentResponse.class);
        when(intent.intentNumber()).thenReturn("INT-1");
        when(payments.createIntentFromBillingDocument(any())).thenReturn(intent);

        var outcome = settlement.settle("SUB-1", "MEMBER", "MBR-1").getFirst();

        InOrder order = inOrder(holdService, payments);
        order.verify(holdService).release("WHL-1", "SYSTEM");
        ArgumentCaptor<CreatePaymentIntentFromBillingDocumentRequest> intents = ArgumentCaptor.forClass(CreatePaymentIntentFromBillingDocumentRequest.class);
        verify(payments, times(2)).createIntentFromBillingDocument(intents.capture());
        assertThat(intents.getAllValues()).extracting(CreatePaymentIntentFromBillingDocumentRequest::documentNumber)
                .containsExactly("INV-1", "INV-2");
        ArgumentCaptor<WalletPaymentRequest> paid = ArgumentCaptor.forClass(WalletPaymentRequest.class);
        verify(payments, times(2)).payWithWallet(any(), paid.capture());
        assertThat(paid.getAllValues()).extracting(WalletPaymentRequest::amount)
                .containsExactly(new BigDecimal("60000"), new BigDecimal("40000"));
        assertThat(paid.getAllValues()).extracting(WalletPaymentRequest::walletNumber).containsOnly("WLT-1");
        assertThat(outcome.returned()).isEqualByComparingTo("0");
        assertThat(outcome.toJson()).contains("\"depositHold\":\"WHL-1\"").contains("INV-2");
    }

    @Test
    @DisplayName("Sans impaye : le depot est rendu en entier au portefeuille")
    void returnsEverythingWithoutDebt() {
        WalletHold hold = WalletHold.builder().holdNumber("WHL-2").amount(new BigDecimal("50000")).currency("XAF")
                .wallet(WalletAccount.builder().walletNumber("WLT-1").build()).build();
        when(holds.findAllByStatusAndSourceTypeAndSourceCode("ACTIVE", "SUBSCRIPTION", "SUB-2")).thenReturn(List.of(hold));
        when(documents.findRecoverableDocuments("MEMBER", "MBR-1")).thenReturn(List.of());

        var outcome = settlement.settle("SUB-2", "MEMBER", "MBR-1").getFirst();

        verify(holdService).release("WHL-2", "SYSTEM");
        verify(payments, times(0)).payWithWallet(any(), any());
        assertThat(outcome.returned()).isEqualByComparingTo("50000");
    }
}
