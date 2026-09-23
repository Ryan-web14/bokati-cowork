package com.sni.bokaticowork.features.payment.mobilemoney;

import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.payment.model.PawapayDeposit;
import com.sni.bokaticowork.features.payment.provider.MobileMoneyPaymentProvider;
import com.sni.bokaticowork.features.payment.provider.MobileMoneyStatusResponse;
import com.sni.bokaticowork.features.payment.provider.pawaypay.PawapayProperties;
import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.PawapayCallbackPayload;
import com.sni.bokaticowork.features.payment.repository.PawapayCallbackRepository;
import com.sni.bokaticowork.features.payment.repository.PawapayDepositRepository;
import com.sni.bokaticowork.features.payment.service.pawaypay.MobileMoneySupervisionService;
import com.sni.bokaticowork.features.payment.service.pawaypay.PawapayCallbackProcessor;
import com.sni.bokaticowork.features.payment.service.pawaypay.PawapayDepositService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La regle de relecture · on ne tranche jamais a la place de l'operateur.
 *
 * <p>Le worker precedent declarait {@code FAILED} au bout de trois tentatives infructueuses. Un
 * client qui validait sa demande une demi-heure plus tard voyait son paiement encaisse par
 * l'operateur et refuse chez nous, avec l'intention annulee par-dessus le marche.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MobileMoneySupervisionServiceTest {

    @Mock private PawapayDepositRepository depositRepository;
    @Mock private PawapayCallbackRepository callbackRepository;
    @Mock private MobileMoneyPaymentProvider provider;
    @Mock private PawapayCallbackProcessor callbackProcessor;
    @Mock private PawapayDepositService depositService;
    @Mock private OutboxService outboxService;

    private PawapayProperties properties;
    private MobileMoneySupervisionService supervision;

    @BeforeEach
    void setUp() {
        properties = new PawapayProperties();
        properties.setPollingMaxHours(24);
        properties.setAlertEmail("tresorerie@elleaose.com, finance@elleaose.com");
        supervision = new MobileMoneySupervisionService(depositRepository, callbackRepository, provider,
                callbackProcessor, depositService, properties, outboxService);
    }

    private PawapayDeposit deposit(String status, Instant createdAt) {
        return PawapayDeposit.builder()
                .depositId("dep-1")
                .intentNumber("PIN-1")
                .transactionNumber("TRX-1")
                .clientReferenceId("TRX-1")
                .phoneNumber("+242061234567")
                .provider("MTN_MOMO_COG")
                .amount(new BigDecimal("5000"))
                .currency("XAF")
                .status(status)
                .createdAt(createdAt)
                .build();
    }

    @Test
    @DisplayName("Un depot confirme par l'operateur est encaisse")
    void settlesASucceededDeposit() {
        when(provider.checkStatus("dep-1"))
                .thenReturn(new MobileMoneyStatusResponse("dep-1", "SUCCEEDED", "ok", null, "prov-9"));

        assertThat(supervision.settle(deposit("PROCESSING", Instant.now())))
                .isEqualTo(MobileMoneySupervisionService.Verdict.SETTLED);

        ArgumentCaptor<PawapayCallbackPayload> captor = ArgumentCaptor.forClass(PawapayCallbackPayload.class);
        verify(callbackProcessor).process(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo("COMPLETED");
        assertThat(captor.getValue().providerTransactionId()).isEqualTo("prov-9");
    }

    @Test
    @DisplayName("Un refus de l'operateur est un echec · c'est lui qui le dit")
    void settlesAFailedDeposit() {
        when(provider.checkStatus("dep-1"))
                .thenReturn(new MobileMoneyStatusResponse("dep-1", "FAILED", "refuse", null, null));

        assertThat(supervision.settle(deposit("PROCESSING", Instant.now())))
                .isEqualTo(MobileMoneySupervisionService.Verdict.SETTLED);

        ArgumentCaptor<PawapayCallbackPayload> captor = ArgumentCaptor.forClass(PawapayCallbackPayload.class);
        verify(callbackProcessor).process(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("Un depot que l'operateur ne connait pas n'est jamais parti · rien n'a ete preleve")
    void treatsNotFoundAsAFailure() {
        when(provider.checkStatus("dep-1"))
                .thenReturn(new MobileMoneyStatusResponse("dep-1", "NOT_FOUND", "inconnu", null, null));

        assertThat(supervision.settle(deposit("PROCESSING", Instant.now())))
                .isEqualTo(MobileMoneySupervisionService.Verdict.SETTLED);
        verify(callbackProcessor).process(any());
    }

    @Test
    @DisplayName("Un depot encore en cours est repousse, sans aucune ecriture de statut")
    void schedulesAProcessingDeposit() {
        when(provider.checkStatus("dep-1"))
                .thenReturn(new MobileMoneyStatusResponse("dep-1", "PROCESSING", "en attente"));

        assertThat(supervision.settle(deposit("PROCESSING", Instant.now())))
                .isEqualTo(MobileMoneySupervisionService.Verdict.PENDING);

        verify(callbackProcessor, never()).process(any());
        verify(depositService).scheduleNextCheck("dep-1", "PROCESSING");
    }

    @Test
    @DisplayName("Une absence de reponse de l'operateur n'est pas un refus")
    void neverWritesOnAnUnknownStatus() {
        when(provider.checkStatus("dep-1"))
                .thenReturn(new MobileMoneyStatusResponse("dep-1", "UNKNOWN", "injoignable"));

        assertThat(supervision.settle(deposit("PROCESSING", Instant.now())))
                .isEqualTo(MobileMoneySupervisionService.Verdict.PENDING);

        verify(callbackProcessor, never()).process(any());
        verify(depositService).scheduleNextCheck("dep-1", "UNKNOWN");
    }

    @Test
    @DisplayName("Au-dela du delai, le depot est a rapprocher · pas echoue · et chaque destinataire est prevenu")
    void marksUnresolvedAndAlertsEveryRecipient() {
        when(provider.checkStatus("dep-1"))
                .thenReturn(new MobileMoneyStatusResponse("dep-1", "PROCESSING", "en attente"));
        PawapayDeposit old = deposit("PROCESSING", Instant.now().minus(30, ChronoUnit.HOURS));
        when(depositService.waitedTooLong(old)).thenReturn(true);

        assertThat(supervision.settle(old)).isEqualTo(MobileMoneySupervisionService.Verdict.UNRESOLVED);

        verify(callbackProcessor, never()).process(any());
        verify(depositService).markUnresolved("dep-1");
        verify(outboxService).publish(eq("MOBILE_MONEY_DEPOSIT_UNRESOLVED"), eq("PAYMENT"),
                eq("dep-1:tresorerie@elleaose.com"), any());
        verify(outboxService).publish(eq("MOBILE_MONEY_DEPOSIT_UNRESOLVED"), eq("PAYMENT"),
                eq("dep-1:finance@elleaose.com"), any());
    }

    @Test
    @DisplayName("Un depot deja a rapprocher ne redeclenche pas une seconde alerte")
    void doesNotAlertTwiceForTheSameDeposit() {
        when(provider.checkStatus("dep-1"))
                .thenReturn(new MobileMoneyStatusResponse("dep-1", "PROCESSING", "en attente"));
        PawapayDeposit alreadyFlagged = deposit("UNRESOLVED", Instant.now().minus(30, ChronoUnit.HOURS));
        when(depositService.waitedTooLong(alreadyFlagged)).thenReturn(true);

        assertThat(supervision.settle(alreadyFlagged)).isEqualTo(MobileMoneySupervisionService.Verdict.UNRESOLVED);

        verify(depositService, never()).markUnresolved(anyString());
        verify(outboxService, never()).publish(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Sans adresse configuree, on n'envoie rien · mais le depot est quand meme marque")
    void marksUnresolvedEvenWithoutARecipient() {
        properties.setAlertEmail("");
        when(provider.checkStatus("dep-1"))
                .thenReturn(new MobileMoneyStatusResponse("dep-1", "PROCESSING", "en attente"));
        PawapayDeposit old = deposit("PROCESSING", Instant.now().minus(30, ChronoUnit.HOURS));
        when(depositService.waitedTooLong(old)).thenReturn(true);

        supervision.settle(old);

        verify(depositService).markUnresolved("dep-1");
        verify(outboxService, never()).publish(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Un depot deja encaisse n'est pas relu · rien ne peut plus changer")
    void recheckLeavesASettledDepositAlone() {
        PawapayDeposit settled = deposit("SUCCEEDED", Instant.now());
        when(depositRepository.findByDepositId("dep-1")).thenReturn(java.util.Optional.of(settled));

        supervision.recheck("dep-1");

        verify(provider, never()).checkStatus(anyString());
        verify(depositService).get("dep-1");
    }

    @Test
    @DisplayName("Un depot a rapprocher se relit a la demande · c'est justement celui qui interesse un agent")
    void recheckRereadsAnUnresolvedDeposit() {
        PawapayDeposit unresolved = deposit("UNRESOLVED", Instant.now().minus(30, ChronoUnit.HOURS));
        when(depositRepository.findByDepositId("dep-1")).thenReturn(java.util.Optional.of(unresolved));
        when(provider.checkStatus("dep-1"))
                .thenReturn(new MobileMoneyStatusResponse("dep-1", "SUCCEEDED", "ok", null, "prov-9"));

        supervision.recheck("dep-1");

        verify(callbackProcessor).process(any());
    }
}
