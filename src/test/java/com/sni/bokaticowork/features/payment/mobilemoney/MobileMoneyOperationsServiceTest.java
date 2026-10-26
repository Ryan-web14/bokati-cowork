package com.sni.bokaticowork.features.payment.mobilemoney;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.payment.model.PawapayDeposit;
import com.sni.bokaticowork.features.payment.provider.pawaypay.PawapayClient;
import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.PawapayCallbackPayload;
import com.sni.bokaticowork.features.payment.repository.PawapayCallbackRepository;
import com.sni.bokaticowork.features.payment.repository.PawapayDepositRepository;
import com.sni.bokaticowork.features.payment.service.pawaypay.MobileMoneyOperationsService;
import com.sni.bokaticowork.features.payment.service.pawaypay.MobileMoneyOperationsService.Action;
import com.sni.bokaticowork.features.payment.service.pawaypay.MobileMoneyOperationsService.Discrepancy;
import com.sni.bokaticowork.features.payment.service.pawaypay.MobileMoneyOperationsService.ProviderView;
import com.sni.bokaticowork.features.payment.service.pawaypay.MobileMoneyResolutionNotifier;
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
import org.springframework.beans.factory.ObjectProvider;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L'outil admin nomme chaque ecart avec PawaPay, et ne force une issue qu'avec ses garde-fous.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MobileMoneyOperationsServiceTest {

    @Mock private PawapayDepositRepository depositRepository;
    @Mock private PawapayCallbackRepository callbackRepository;
    @Mock private PawapayDepositService depositService;
    @Mock private MobileMoneySupervisionService supervision;
    @Mock private PawapayCallbackProcessor callbackProcessor;
    @Mock private MobileMoneyResolutionNotifier notifier;
    @Mock private ObjectProvider<PawapayClient> clientProvider;

    private MobileMoneyOperationsService service;

    @BeforeEach
    void setUp() {
        service = new MobileMoneyOperationsService(depositRepository, callbackRepository, depositService, supervision,
                callbackProcessor, notifier, clientProvider);
        when(callbackRepository.findByReferenceIdOrderByReceivedAtDesc(anyString())).thenReturn(List.of());
    }

    private static PawapayDeposit deposit(String status) {
        PawapayDeposit deposit = new PawapayDeposit();
        deposit.setDepositId("dep-1");
        deposit.setIntentNumber("PI-1");
        deposit.setClientReferenceId("TX-1");
        deposit.setStatus(status);
        deposit.setAmount(new BigDecimal("10.0000"));
        deposit.setCurrency("XAF");
        return deposit;
    }

    private static ProviderView pawapay(String status, String amount) {
        return new ProviderView(true, !"NOT_FOUND".equals(status), status, amount == null ? null : new BigDecimal(amount),
                "XAF", "8442462698", null, null, null);
    }

    @SuppressWarnings("unchecked")
    private static List<Discrepancy> discrepancies(PawapayDeposit deposit, String transactionStatus, ProviderView view) throws Exception {
        Method method = MobileMoneyOperationsService.class.getDeclaredMethod("discrepancies",
                PawapayDeposit.class, String.class, ProviderView.class);
        method.setAccessible(true);
        return (List<Discrepancy>) method.invoke(null, deposit, transactionStatus, view);
    }

    @Test
    @DisplayName("Paye chez PawaPay, en attente chez nous · l'ecart du rappel perdu, resolu par SYNC")
    void paidAtProviderPendingHere() throws Exception {
        List<Discrepancy> found = discrepancies(deposit("PROCESSING"), "PROCESSING", pawapay("COMPLETED", "10.00"));

        assertThat(found).extracting(Discrepancy::code).containsExactly("PAID_AT_PROVIDER_ONLY");
        assertThat(found.getFirst().suggestedAction()).isEqualTo("SYNC");
    }

    @Test
    @DisplayName("Paye chez nous, echoue ou inconnu chez PawaPay · ecart critique")
    void paidHereOnly() throws Exception {
        assertThat(discrepancies(deposit("SUCCEEDED"), "SUCCEEDED", pawapay("FAILED", "10.00")))
                .extracting(Discrepancy::code).containsExactly("PAID_HERE_ONLY");
        assertThat(discrepancies(deposit("SUCCEEDED"), "SUCCEEDED", pawapay("NOT_FOUND", null)))
                .extracting(Discrepancy::severity).containsExactly("CRITICAL");
    }

    @Test
    @DisplayName("Un montant different est critique · 10.00 chez PawaPay et 10.0000 chez nous concordent")
    void amountMismatch() throws Exception {
        assertThat(discrepancies(deposit("SUCCEEDED"), "SUCCEEDED", pawapay("COMPLETED", "10.00"))).isEmpty();
        assertThat(discrepancies(deposit("SUCCEEDED"), "SUCCEEDED", pawapay("COMPLETED", "100.00")))
                .extracting(Discrepancy::code).containsExactly("AMOUNT_MISMATCH");
    }

    @Test
    @DisplayName("Depot reussi mais transaction non reglee · incoherence interne signalee")
    void transactionNotSettled() throws Exception {
        assertThat(discrepancies(deposit("SUCCEEDED"), "PROCESSING", pawapay("COMPLETED", "10")))
                .extracting(Discrepancy::code).containsExactly("TRANSACTION_NOT_SETTLED");
    }

    @Test
    @DisplayName("SYNC relit PawaPay et applique sa reponse")
    void syncRechecks() {
        when(depositRepository.findByDepositId("dep-1")).thenReturn(Optional.of(deposit("PROCESSING")));

        service.resolve("dep-1", Action.SYNC, null, null, "admin@x.com");

        verify(supervision).recheck("dep-1");
        verify(callbackProcessor, never()).process(any());
    }

    @Test
    @DisplayName("Forcer un succes exige un motif et la reference de l'operateur, puis passe par le traitement des rappels")
    void forcingSuccessRequiresProof() {
        when(depositRepository.findByDepositId("dep-1")).thenReturn(Optional.of(deposit("UNRESOLVED")));

        assertThatThrownBy(() -> service.resolve("dep-1", Action.MARK_SUCCEEDED, "court", "REF", "admin"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("10 caractères");
        assertThatThrownBy(() -> service.resolve("dep-1", Action.MARK_SUCCEEDED, "Releve MTN du 26/10 fourni par le client", null, "admin"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("référence");

        service.resolve("dep-1", Action.MARK_SUCCEEDED, "Releve MTN du 26/10 fourni par le client", "8442462698", "admin@x.com");

        ArgumentCaptor<PawapayCallbackPayload> payload = ArgumentCaptor.forClass(PawapayCallbackPayload.class);
        verify(callbackProcessor).process(payload.capture());
        assertThat(payload.getValue().status()).isEqualTo("COMPLETED");
        assertThat(payload.getValue().providerTransactionId()).isEqualTo("8442462698");
        verify(notifier).resolved(any(), eq("MARK_SUCCEEDED"), anyString(), eq("8442462698"), eq("admin@x.com"));
    }

    @Test
    @DisplayName("Un paiement deja reussi ne se force pas en echec · c'est un remboursement")
    void settledPaymentCannotBeForced() {
        when(depositRepository.findByDepositId("dep-1")).thenReturn(Optional.of(deposit("SUCCEEDED")));

        assertThatThrownBy(() -> service.resolve("dep-1", Action.MARK_FAILED, "Le client conteste ce paiement", null, "admin"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("remboursement");
        verify(callbackProcessor, never()).process(any());
    }
}
