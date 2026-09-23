package com.sni.bokaticowork.features.payment.mobilemoney;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.features.payment.model.PawapayCallback;
import com.sni.bokaticowork.features.payment.provider.MobileMoneyPaymentProvider;
import com.sni.bokaticowork.features.payment.provider.MobileMoneyStatusResponse;
import com.sni.bokaticowork.features.payment.provider.pawaypay.PawapayClient;
import com.sni.bokaticowork.features.payment.provider.pawaypay.PawapaySignatureVerifier;
import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.PawapayCallbackPayload;
import com.sni.bokaticowork.features.payment.service.pawaypay.PawapayCallbackGateway;
import com.sni.bokaticowork.features.payment.service.pawaypay.PawapayCallbackJournal;
import com.sni.bokaticowork.features.payment.service.pawaypay.PawapayCallbackProcessor;
import com.sni.bokaticowork.features.payment.service.pawaypay.PawapayDepositService;
import com.sni.bokaticowork.features.payment.service.pawaypay.PawapayRefundCallbackProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Un rappel arrive par une URL publique · il ne fait encaisser personne sur sa seule parole.
 *
 * <p>La route acceptait auparavant n'importe quel corps JSON portant {@code status: COMPLETED} :
 * un tiers connaissant l'URL et un identifiant de depot faisait passer une facture en payee. Ici
 * on verifie que seule une signature valide, ou le statut relu chez l'operateur, declenche une
 * ecriture · et que tout est consigne.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PawapayCallbackGatewayTest {

    @Mock private PawapayCallbackJournal journal;
    @Mock private PawapaySignatureVerifier verifier;
    @Mock private PawapayCallbackProcessor callbackProcessor;
    @Mock private PawapayRefundCallbackProcessor refundProcessor;
    @Mock private PawapayDepositService depositService;
    @Mock private MobileMoneyPaymentProvider provider;
    @Mock private ObjectProvider<PawapayClient> clientProvider;

    private PawapayCallbackGateway gateway;

    private static final String DEPOSIT_BODY = "{\"depositId\":\"dep-1\",\"status\":\"COMPLETED\"}";

    @BeforeEach
    void setUp() {
        gateway = new PawapayCallbackGateway(journal, verifier, callbackProcessor, refundProcessor,
                depositService, provider, clientProvider, new ObjectMapper());
        when(journal.open(any(), anyString(), anyBoolean(), any())).thenReturn(7L);
    }

    @Test
    @DisplayName("Une signature valide suffit · le corps du rappel fait foi")
    void processesASignedCallback() {
        when(verifier.verify(DEPOSIT_BODY, "sha256=abc")).thenReturn(true);

        assertThat(gateway.receiveDeposit(DEPOSIT_BODY, "sha256=abc", "10.0.0.1"))
                .isEqualTo(PawapayCallback.Outcome.PROCESSED);

        ArgumentCaptor<PawapayCallbackPayload> captor = ArgumentCaptor.forClass(PawapayCallbackPayload.class);
        verify(callbackProcessor).process(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo("COMPLETED");
        verify(journal).close(eq(7L), eq("dep-1"), eq("COMPLETED"),
                eq(PawapayCallback.VerifiedVia.SIGNATURE), eq(PawapayCallback.Outcome.PROCESSED), any());
        verify(provider, never()).checkStatus(anyString());
    }

    @Test
    @DisplayName("Sans signature, c'est l'operateur qui tranche · pas le corps recu")
    void reconfirmsAnUnsignedCallbackWithTheProvider() {
        when(verifier.verify(anyString(), any())).thenReturn(false);
        when(provider.checkStatus("dep-1"))
                .thenReturn(new MobileMoneyStatusResponse("dep-1", "SUCCEEDED", "ok", null, "prov-9"));

        assertThat(gateway.receiveDeposit(DEPOSIT_BODY, null, "10.0.0.1"))
                .isEqualTo(PawapayCallback.Outcome.PROCESSED);

        verify(provider).checkStatus("dep-1");
        verify(journal).close(eq(7L), eq("dep-1"), eq("COMPLETED"),
                eq(PawapayCallback.VerifiedVia.PROVIDER_STATUS), eq(PawapayCallback.Outcome.PROCESSED), any());
    }

    @Test
    @DisplayName("Un rappel forge qui annonce un paiement que l'operateur dement n'encaisse rien")
    void ignoresAForgedCallback() {
        when(verifier.verify(anyString(), any())).thenReturn(false);
        when(provider.checkStatus("dep-1"))
                .thenReturn(new MobileMoneyStatusResponse("dep-1", "NOT_FOUND", "inconnu"));

        assertThat(gateway.receiveDeposit(DEPOSIT_BODY, null, "10.0.0.1"))
                .isEqualTo(PawapayCallback.Outcome.IGNORED);

        verify(callbackProcessor, never()).process(any());
    }

    @Test
    @DisplayName("Un depot encore en cours chez l'operateur est remis a plus tard, sans ecriture")
    void defersAPendingDeposit() {
        when(verifier.verify(anyString(), any())).thenReturn(false);
        when(provider.checkStatus("dep-1"))
                .thenReturn(new MobileMoneyStatusResponse("dep-1", "PROCESSING", "en attente"));

        assertThat(gateway.receiveDeposit(DEPOSIT_BODY, null, "10.0.0.1"))
                .isEqualTo(PawapayCallback.Outcome.DEFERRED);

        verify(callbackProcessor, never()).process(any());
        verify(depositService).scheduleNextCheck("dep-1", "PROCESSING");
    }

    @Test
    @DisplayName("Un operateur injoignable ne fait rien echouer · le worker reprendra")
    void defersWhenTheProviderIsUnreachable() {
        when(verifier.verify(anyString(), any())).thenReturn(false);
        when(provider.checkStatus("dep-1")).thenThrow(new PawapayClient.ProviderUnreachableException("injoignable", null));

        assertThat(gateway.receiveDeposit(DEPOSIT_BODY, null, "10.0.0.1"))
                .isEqualTo(PawapayCallback.Outcome.DEFERRED);

        verify(callbackProcessor, never()).process(any());
        verify(depositService).scheduleNextCheck("dep-1", "UNKNOWN");
    }

    @Test
    @DisplayName("Un corps illisible est consigne, pas avale en silence")
    void recordsAnUnreadableBody() {
        assertThat(gateway.receiveDeposit("pas du json", "sha256=abc", "10.0.0.1"))
                .isEqualTo(PawapayCallback.Outcome.FAILED);

        verify(journal).close(eq(7L), eq(null), eq(null), eq(PawapayCallback.VerifiedVia.NONE),
                eq(PawapayCallback.Outcome.FAILED), any());
        verify(callbackProcessor, never()).process(any());
    }

    @Test
    @DisplayName("Un rappel sans identifiant ne designe rien · il ne fait rien")
    void ignoresACallbackWithoutADepositId() {
        assertThat(gateway.receiveDeposit("{\"status\":\"COMPLETED\"}", "sha256=abc", "10.0.0.1"))
                .isEqualTo(PawapayCallback.Outcome.IGNORED);

        verify(callbackProcessor, never()).process(any());
    }

    @Test
    @DisplayName("Un echec de traitement laisse une trace · un paiement perdu doit pouvoir s'expliquer")
    void recordsAProcessingFailure() {
        when(verifier.verify(anyString(), any())).thenReturn(true);
        org.mockito.Mockito.doThrow(new IllegalStateException("base indisponible"))
                .when(callbackProcessor).process(any());

        assertThat(gateway.receiveDeposit(DEPOSIT_BODY, "sha256=abc", "10.0.0.1"))
                .isEqualTo(PawapayCallback.Outcome.FAILED);

        verify(journal).close(eq(7L), eq("dep-1"), eq("COMPLETED"), eq(PawapayCallback.VerifiedVia.NONE),
                eq(PawapayCallback.Outcome.FAILED), any());
    }

    @Test
    @DisplayName("Un remboursement non signe n'est plus jete · son statut est relu chez l'operateur")
    void reconfirmsAnUnsignedRefund() {
        PawapayClient client = org.mockito.Mockito.mock(PawapayClient.class);
        when(clientProvider.getIfAvailable()).thenReturn(client);
        when(verifier.verify(anyString(), any())).thenReturn(false);
        when(client.getRefundStatus("ref-1"))
                .thenReturn(new PawapayClient.DepositStatus("ref-1", true, "COMPLETED", null, null, null));

        assertThat(gateway.receiveRefund("{\"refundId\":\"ref-1\",\"status\":\"COMPLETED\"}", null, "10.0.0.1"))
                .isEqualTo(PawapayCallback.Outcome.PROCESSED);

        verify(refundProcessor).process(any());
    }

    @Test
    @DisplayName("Sans acces a l'operateur, un remboursement non signe reste sans effet")
    void ignoresAnUnsignedRefundWithoutAProviderClient() {
        when(clientProvider.getIfAvailable()).thenReturn(null);
        when(verifier.verify(anyString(), any())).thenReturn(false);

        assertThat(gateway.receiveRefund("{\"refundId\":\"ref-1\",\"status\":\"COMPLETED\"}", null, "10.0.0.1"))
                .isEqualTo(PawapayCallback.Outcome.IGNORED);

        verify(refundProcessor, never()).process(any());
    }
}
