package com.sni.bokaticowork.features.payment.mobilemoney;

import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.payment.model.PawapayDeposit;
import com.sni.bokaticowork.features.payment.service.pawaypay.MobileMoneyClientNotifier;
import com.sni.bokaticowork.features.payment.service.support.TransactionContextResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Un client dont le paiement echoue doit l'apprendre, et savoir quoi faire ensuite.
 *
 * <p>Le refus arrive souvent apres qu'il a ferme sa page. Sans courriel, il voyait sa facture
 * rester ouverte sans explication · et dans le cas non tranche, il repayait.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MobileMoneyClientNotifierTest {

    @Mock private OutboxService outboxService;
    @Mock private TransactionContextResolver contextResolver;
    @InjectMocks private MobileMoneyClientNotifier notifier;

    private PawapayDeposit deposit(String failureCode, String userMessage, Boolean retryable) {
        return PawapayDeposit.builder()
                .depositId("dep-1")
                .intentNumber("PIN-1")
                .transactionNumber("TRX-1")
                .customerType("MEMBER")
                .customerCode("MBR-1")
                .phoneNumber("+242061234567")
                .provider("MTN_MOMO_COG")
                .amount(new BigDecimal("5000.0000"))
                .currency("XAF")
                .failureCode(failureCode)
                .userMessage(userMessage)
                .retryable(retryable)
                .build();
    }

    private void memberIsReachable() {
        when(contextResolver.resolveParty("MEMBER", "MBR-1")).thenReturn(
                new TransactionContextResolver.PartyView("MEMBER", "MBR-1", "Joël Bikindou",
                        "joel@example.com", "+242061234567", null, true));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> captured(String eventType) {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(outboxService).publish(eq(eventType), eq("PAYMENT"), eq("dep-1"), captor.capture());
        return (Map<String, Object>) captor.getValue();
    }

    @Test
    @DisplayName("L'echec dit pourquoi, en francais, et si reessayer a un sens")
    void failureCarriesTheReasonAndTheAdvice() {
        memberIsReachable();

        notifier.depositFailed(deposit("INSUFFICIENT_BALANCE",
                "Le solde de votre compte mobile money est insuffisant. Rechargez-le puis réessayez.", true));

        Map<String, Object> payload = captured("MOBILE_MONEY_DEPOSIT_FAILED");
        assertThat(payload).containsEntry("recipientEmail", "joel@example.com")
                .containsEntry("recipientName", "Joël Bikindou")
                .containsEntry("recipientType", "MEMBER")
                .containsEntry("failureCode", "INSUFFICIENT_BALANCE")
                .containsEntry("retryable", true)
                .containsEntry("amount", "5000")
                .containsEntry("currency", "XAF")
                .containsEntry("templateCode", "MOBILE_MONEY_DEPOSIT_FAILED");
        assertThat((String) payload.get("userMessage")).contains("solde");
    }

    @Test
    @DisplayName("Le nom de l'operateur remplace son code · personne ne lit MTN_MOMO_COG")
    void theOperatorIsNamedNotCoded() {
        memberIsReachable();

        notifier.depositFailed(deposit("PAYER_NOT_FOUND", "Ce numéro n'est pas un compte actif.", false));

        assertThat(captured("MOBILE_MONEY_DEPOSIT_FAILED"))
                .containsEntry("provider", "MTN Mobile Money Congo")
                .containsEntry("retryable", false);
    }

    @Test
    @DisplayName("Sans explication de l'operateur, le message reste une phrase, pas un vide")
    void alwaysCarriesAReadableMessage() {
        memberIsReachable();

        notifier.depositFailed(deposit(null, null, null));

        Map<String, Object> payload = captured("MOBILE_MONEY_DEPOSIT_FAILED");
        assertThat((String) payload.get("userMessage")).isNotBlank();
        assertThat(payload).containsEntry("retryable", false);
    }

    @Test
    @DisplayName("Le depot non tranche dit surtout de ne pas payer deux fois")
    void pendingReviewIsSentToTheClient() {
        memberIsReachable();

        notifier.depositPendingReview(deposit("UNRESOLVED",
                "Nous n'avons pas reçu de réponse définitive de l'opérateur.", false));

        Map<String, Object> payload = captured("MOBILE_MONEY_DEPOSIT_PENDING_REVIEW");
        assertThat(payload).containsEntry("recipientEmail", "joel@example.com")
                .containsEntry("templateCode", "MOBILE_MONEY_DEPOSIT_PENDING_REVIEW");
    }

    @Test
    @DisplayName("Sans adresse joignable, rien ne part · et rien ne casse")
    void staysSilentWithoutAnAddress() {
        when(contextResolver.resolveParty(anyString(), anyString()))
                .thenReturn(TransactionContextResolver.PartyView.unregistered("MEMBER", "MBR-1"));

        notifier.depositFailed(deposit("OTHER_ERROR", "Le paiement a échoué.", true));
        notifier.depositPendingReview(deposit("UNRESOLVED", "Sans réponse.", false));

        verify(outboxService, never()).publish(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Une entreprise est un destinataire connu du module de notification")
    void mapsBusinessEntityToAKnownRecipientType() {
        PawapayDeposit business = deposit("OTHER_ERROR", "Le paiement a échoué.", true);
        business.setCustomerType("BUSINESS_ENTITY");
        business.setCustomerCode("BIZ-1");
        when(contextResolver.resolveParty("BUSINESS_ENTITY", "BIZ-1")).thenReturn(
                new TransactionContextResolver.PartyView("BUSINESS_ENTITY", "BIZ-1", "SARL Mboté",
                        "contact@mbote.cg", null, null, true));

        notifier.depositFailed(business);

        assertThat(captured("MOBILE_MONEY_DEPOSIT_FAILED")).containsEntry("recipientType", "BUSINESS");
    }
}
