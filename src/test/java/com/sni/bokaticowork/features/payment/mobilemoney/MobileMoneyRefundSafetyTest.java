package com.sni.bokaticowork.features.payment.mobilemoney;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.dto.request.RefundPaymentRequest;
import com.sni.bokaticowork.features.payment.enums.PaymentMethod;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.provider.MobileMoneyPaymentProvider;
import com.sni.bokaticowork.features.payment.provider.MobileMoneyRefundResponse;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import com.sni.bokaticowork.features.payment.service.implementation.PaymentServiceImpl;
import com.sni.bokaticowork.features.payment.service.support.PaymentAllocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Un remboursement mobile money ne rouvre la facture qu'une fois confirme, et ne part pas deux fois.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MobileMoneyRefundSafetyTest {

    @Mock private PaymentTransactionRepository transactionRepository;
    @Mock private PaymentAllocationService allocationService;
    @Mock private MobileMoneyPaymentProvider mobileMoneyProvider;
    @Mock private SequenceGeneratorFacade sequenceGenerator;
    @Mock private com.sni.bokaticowork.features.payment.mapper.interfaces.PaymentMapper mapper;
    @Mock private com.sni.bokaticowork.features.payment.repository.PaymentIntentRepository intentRepository;
    @Mock private com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService outboxService;
    @org.mockito.Spy private com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
    @InjectMocks private PaymentServiceImpl service;

    private PaymentTransaction original;

    @BeforeEach
    void setUp() {
        PaymentIntent intent = new PaymentIntent();
        intent.setIntentNumber("PI-1");
        original = new PaymentTransaction();
        original.setTransactionNumber("TXN-1");
        original.setPaymentIntent(intent);
        original.setPaymentMethod(PaymentMethod.MOBILE_MONEY);
        original.setProviderReference("dep-1");
        original.setAmount(new BigDecimal("10"));
        original.setCurrency("XAF");
        original.setStatus(PaymentTransactionStatus.SUCCEEDED);
        when(transactionRepository.findByTransactionNumber("TXN-1")).thenReturn(Optional.of(original));
        when(transactionRepository.findAllByOriginalTransactionNumber("TXN-1")).thenReturn(List.of());
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(sequenceGenerator.next(anyString())).thenReturn("SEQ-0000042");
    }

    private static RefundPaymentRequest request() {
        return new RefundPaymentRequest(null, "Annulation de la reservation", "admin@x.com");
    }

    @Test
    @DisplayName("La facture n'est pas rouverte a l'envoi du remboursement, seulement a sa confirmation")
    void allocationsStayUntilConfirmation() {
        when(mobileMoneyProvider.refund(any())).thenReturn(new MobileMoneyRefundResponse("dep-1", "ref-1", "PROCESSING", "envoye"));

        service.refund("TXN-1", request());

        verify(allocationService, never()).reverseAllocations(any(), any());
    }

    @Test
    @DisplayName("Un remboursement en cours bloque un second remboursement du meme montant")
    void inFlightRefundBlocksASecondOne() {
        PaymentTransaction inFlight = new PaymentTransaction();
        inFlight.setTransactionNumber("TXN-R1");
        inFlight.setStatus(PaymentTransactionStatus.PROCESSING);
        inFlight.setAmount(new BigDecimal("10"));
        when(transactionRepository.findAllByOriginalTransactionNumber("TXN-1")).thenReturn(List.of(inFlight));

        assertThatThrownBy(() -> service.refund("TXN-1", request())).isInstanceOf(ConflictException.class);
        verify(mobileMoneyProvider, never()).refund(any());
    }

    @Test
    @DisplayName("Un refus de l'operateur est une exception dediee, et la facture reste payee")
    void rejectionIsTracedAndInvoiceStaysPaid() {
        when(mobileMoneyProvider.refund(any())).thenReturn(new MobileMoneyRefundResponse("dep-1", null, "FAILED", "solde insuffisant"));

        assertThatThrownBy(() -> service.refund("TXN-1", request()))
                .isInstanceOf(PaymentServiceImpl.MobileMoneyRefundRejectedException.class)
                .hasMessageContaining("solde insuffisant");
        verify(allocationService, never()).reverseAllocations(any(), any());
    }

    @Test
    @DisplayName("Un remboursement en especes, synchrone, rouvre la facture tout de suite comme avant")
    void cashRefundStillReversesAtOnce() {
        original.setPaymentMethod(PaymentMethod.CASH);
        original.setProviderReference(null);

        service.refund("TXN-1", request());

        verify(allocationService).reverseAllocations(original, new BigDecimal("10"));
    }
}
