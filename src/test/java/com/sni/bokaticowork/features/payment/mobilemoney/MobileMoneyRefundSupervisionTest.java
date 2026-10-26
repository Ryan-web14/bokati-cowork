package com.sni.bokaticowork.features.payment.mobilemoney;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.provider.pawaypay.PawapayClient;
import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.PawapayRefundCallbackPayload;
import com.sni.bokaticowork.features.payment.repository.PaymentIntentRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import com.sni.bokaticowork.features.payment.service.pawaypay.MobileMoneyRefundSupervision;
import com.sni.bokaticowork.features.payment.service.pawaypay.PawapayRefundCallbackProcessor;
import com.sni.bokaticowork.features.payment.service.support.PaymentAllocationService;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Un remboursement reste en cours n'y reste plus · et sa confirmation rouvre la bonne facture.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MobileMoneyRefundSupervisionTest {

    @Mock private PaymentTransactionRepository transactionRepository;
    @Mock private PawapayRefundCallbackProcessor refundProcessor;
    @Mock private ObjectProvider<PawapayClient> clientProvider;
    @Mock private PawapayClient client;

    private MobileMoneyRefundSupervision supervision;
    private PaymentTransaction refund;

    @BeforeEach
    void setUp() {
        supervision = new MobileMoneyRefundSupervision(transactionRepository, refundProcessor, clientProvider);
        when(clientProvider.getIfAvailable()).thenReturn(client);
        refund = new PaymentTransaction();
        refund.setTransactionNumber("TXN-R1");
        refund.setProviderReference("ref-1");
        refund.setStatus(PaymentTransactionStatus.PROCESSING);
        refund.setCreatedAt(Instant.now().minusSeconds(120));
        when(transactionRepository.findPendingMobileMoneyRefunds(any(), anyInt())).thenReturn(List.of(refund));
    }

    private void pawapaySays(boolean found, String status) {
        when(client.getRefundStatus("ref-1")).thenReturn(
                new PawapayClient.DepositStatus("ref-1", found, status, null, null, null));
    }

    private String appliedStatus() {
        ArgumentCaptor<PawapayRefundCallbackPayload> payload = ArgumentCaptor.forClass(PawapayRefundCallbackPayload.class);
        verify(refundProcessor).process(payload.capture());
        return payload.getValue().status();
    }

    @Test
    @DisplayName("Confirme chez PawaPay : applique comme un rappel COMPLETED")
    void completedIsApplied() {
        pawapaySays(true, "COMPLETED");
        assertThat(supervision.sweep(10)).isEqualTo(1);
        assertThat(appliedStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("Refuse chez PawaPay : applique comme un rappel FAILED")
    void failedIsApplied() {
        pawapaySays(true, "REJECTED");
        supervision.sweep(10);
        assertThat(appliedStatus()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("Inconnu de PawaPay juste apres l'envoi : on attend ; inconnu apres 15 minutes : echec")
    void notFoundWaitsThenFails() {
        pawapaySays(false, "NOT_FOUND");
        supervision.sweep(10);
        verify(refundProcessor, never()).process(any());

        refund.setCreatedAt(Instant.now().minusSeconds(3600));
        supervision.sweep(10);
        assertThat(appliedStatus()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("Encore en cours chez PawaPay : rien n'est ecrit")
    void processingWritesNothing() {
        pawapaySays(true, "PROCESSING");
        assertThat(supervision.sweep(10)).isZero();
        verify(refundProcessor, never()).process(any());
    }

    /** Le rappel de remboursement confirme rouvre la facture de la transaction remboursee, pas d'une autre. */
    @Test
    @DisplayName("La confirmation annule l'affectation de la transaction remboursee, retrouvee par son lien")
    void confirmationReversesTheRightAllocation() {
        PaymentAllocationService allocationService = org.mockito.Mockito.mock(PaymentAllocationService.class);
        PawapayRefundCallbackProcessor processor = new PawapayRefundCallbackProcessor(transactionRepository,
                org.mockito.Mockito.mock(PaymentIntentRepository.class), org.mockito.Mockito.mock(OutboxService.class),
                allocationService, new ObjectMapper());
        PaymentIntent intent = new PaymentIntent();
        intent.setId(1L);
        refund.setPaymentIntent(intent);
        refund.setAmount(new BigDecimal("10"));
        refund.setMetadataJson("{\"originalTransactionNumber\":\"TXN-1\"}");
        PaymentTransaction original = new PaymentTransaction();
        original.setTransactionNumber("TXN-1");
        original.setAmount(new BigDecimal("10"));
        when(transactionRepository.findByProviderReference("ref-1")).thenReturn(Optional.of(refund));
        when(transactionRepository.findByTransactionNumber("TXN-1")).thenReturn(Optional.of(original));
        when(transactionRepository.findAllByOriginalTransactionNumber("TXN-1")).thenReturn(List.of());

        processor.process(new PawapayRefundCallbackPayload("ref-1", null, "COMPLETED", null, null, null, null, null));

        verify(allocationService).reverseAllocations(original, new BigDecimal("10"));
        assertThat(refund.getStatus()).isEqualTo(PaymentTransactionStatus.REFUNDED);
    }
}
