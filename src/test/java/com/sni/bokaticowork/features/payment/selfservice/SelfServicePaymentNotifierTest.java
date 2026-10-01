package com.sni.bokaticowork.features.payment.selfservice;

import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.payment.enums.PaymentChannel;
import com.sni.bokaticowork.features.payment.enums.PaymentMethod;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.service.support.SelfServicePaymentNotifier;
import com.sni.bokaticowork.features.payment.service.support.TransactionContextResolver;
import com.sni.bokaticowork.features.portal.notification.service.AdminInAppNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La caisse apprend qu'un client a regle tout seul · et seulement dans ce cas.
 *
 * <p>Un reglement fait depuis l'espace client n'etait annonce a personne : on le decouvrait en
 * ouvrant la facture. Entre-temps le client se presentait et on lui refusait l'acces, ou il
 * recevait une relance pour une facture deja payee.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SelfServicePaymentNotifierTest {

    @Mock private OutboxService outboxService;
    @Mock private AdminInAppNotifier adminInAppNotifier;
    @Mock private TransactionContextResolver contextResolver;
    @InjectMocks private SelfServicePaymentNotifier notifier;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notifier, "confirmationEmail", "caisse@elleaose.com, direction@elleaose.com");
        when(contextResolver.resolveParty("MEMBER", "MBR-1")).thenReturn(
                new TransactionContextResolver.PartyView("MEMBER", "MBR-1", "Joël Bikindou",
                        "joel@example.com", "+242061234567", null, true));
        when(contextResolver.resolveSource(anyString(), anyString())).thenReturn(
                new TransactionContextResolver.SourceView("BILLING_DOCUMENT", "INV-1", "Facture INV-1", true));
    }

    private PaymentTransaction transaction(PaymentChannel channel, PaymentMethod method) {
        PaymentIntent intent = new PaymentIntent();
        intent.setIntentNumber("PIN-1");
        intent.setCustomerType("MEMBER");
        intent.setCustomerCode("MBR-1");
        intent.setSourceType("BILLING_DOCUMENT");
        intent.setSourceCode("INV-1");
        return PaymentTransaction.builder()
                .transactionNumber("TRX-1")
                .receiptNumber("REC-1")
                .paymentIntent(intent)
                .paymentMethod(method)
                .channel(channel)
                .provider("PAWAYPAY")
                .amount(new BigDecimal("25000.0000"))
                .currency("XAF")
                .status(PaymentTransactionStatus.SUCCEEDED)
                .paidAt(Instant.parse("2026-09-23T10:15:30Z"))
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> payloadFor(String recipient) {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(outboxService).publish(eq("SELF_SERVICE_PAYMENT_RECEIVED"), eq("PAYMENT"),
                eq("TRX-1:" + recipient), captor.capture());
        return (Map<String, Object>) captor.getValue();
    }

    @Test
    @DisplayName("Chaque destinataire recoit le courriel et la notification du portail")
    void notifiesEveryConfiguredRecipient() {
        notifier.paymentSettled(transaction(PaymentChannel.SELF_SERVICE, PaymentMethod.MOBILE_MONEY));

        Map<String, Object> payload = payloadFor("caisse@elleaose.com");
        assertThat(payload).containsEntry("adminEmail", "caisse@elleaose.com")
                .containsEntry("transactionNumber", "TRX-1")
                .containsEntry("receiptNumber", "REC-1")
                .containsEntry("amount", "25000")
                .containsEntry("currency", "XAF")
                .containsEntry("paymentMethod", "Mobile money")
                .containsEntry("customerName", "Joël Bikindou")
                .containsEntry("purpose", "Facture INV-1")
                .containsEntry("templateCode", "SELF_SERVICE_PAYMENT_RECEIVED");

        assertThat(payloadFor("direction@elleaose.com")).containsEntry("adminEmail", "direction@elleaose.com");

        verify(adminInAppNotifier).notify(eq("SELF_SERVICE_PAYMENT_RECEIVED"), eq("PAYMENT"), eq("TRX-1"),
                eq("caisse@elleaose.com"), anyString(), any());
        verify(adminInAppNotifier).notify(eq("SELF_SERVICE_PAYMENT_RECEIVED"), eq("PAYMENT"), eq("TRX-1"),
                eq("direction@elleaose.com"), anyString(), any());
    }

    @Test
    @DisplayName("L'ecran de caisse ouvert s'allume tout de suite")
    void broadcastsToTheOpenDeskScreen() {
        notifier.paymentSettled(transaction(PaymentChannel.SELF_SERVICE, PaymentMethod.WALLET));

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(adminInAppNotifier).broadcastAlert(eq("SELF_SERVICE_PAYMENT_RECEIVED"), eq("PAYMENT"),
                anyString(), message.capture(), eq("INFO"), any());
        assertThat(message.getValue()).contains("Joël Bikindou").contains("25000").contains("Portefeuille");
    }

    @Test
    @DisplayName("Un paiement saisi au guichet ne previent personne · quelqu'un etait deja en face")
    void staysSilentForBackOfficePayments() {
        notifier.paymentSettled(transaction(PaymentChannel.BACK_OFFICE, PaymentMethod.CASH));

        verify(outboxService, never()).publish(anyString(), anyString(), anyString(), any());
        verify(adminInAppNotifier, never()).broadcastAlert(anyString(), anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Une ecriture automatique non plus · il n'y a pas de client a servir derriere")
    void staysSilentForSystemPayments() {
        notifier.paymentSettled(transaction(PaymentChannel.SYSTEM, PaymentMethod.WALLET));
        notifier.paymentSettled(transaction(null, PaymentMethod.WALLET));

        verify(outboxService, never()).publish(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Sans adresse configuree, la notification du portail part quand meme")
    void stillBroadcastsWithoutAnEmailRecipient() {
        ReflectionTestUtils.setField(notifier, "confirmationEmail", "");

        notifier.paymentSettled(transaction(PaymentChannel.SELF_SERVICE, PaymentMethod.MOBILE_MONEY));

        verify(outboxService, never()).publish(anyString(), anyString(), anyString(), any());
        verify(adminInAppNotifier).broadcastAlert(anyString(), anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Prevenir la caisse ne fait jamais echouer un encaissement · l'argent est recu")
    void neverFailsTheSettlement() {
        org.mockito.Mockito.doThrow(new IllegalStateException("outbox indisponible"))
                .when(outboxService).publish(anyString(), anyString(), anyString(), any());

        notifier.paymentSettled(transaction(PaymentChannel.SELF_SERVICE, PaymentMethod.MOBILE_MONEY));
    }
}
