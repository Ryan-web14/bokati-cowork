package com.sni.bokaticowork.features.payment.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.enums.CashSessionStatus;
import com.sni.bokaticowork.features.payment.enums.PaymentMethod;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;
import com.sni.bokaticowork.features.payment.mapper.interfaces.CashRegisterMapper;
import com.sni.bokaticowork.features.payment.model.CashMovement;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.repository.CashMovementRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterRepository;
import com.sni.bokaticowork.features.payment.repository.CashSessionRepository;
import com.sni.bokaticowork.features.payment.service.support.CashSessionSummarySupport;
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

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le mobile money de la plateforme entre seul dans sa caisse automatique, qu'une session y soit
 * ouverte ou non ; le mobile money saisi au guichet n'y entre pas ; un paiement hors especes se
 * rattache a une session de guichet ouverte, jamais a une caisse automatique.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CounterAndAutomaticRegisterTest {

    @Mock private CashRegisterRepository registerRepository;
    @Mock private CashSessionRepository sessionRepository;
    @Mock private CashMovementRepository movementRepository;
    @Mock private SequenceGeneratorFacade sequenceGenerator;
    @Mock private CashSessionSummarySupport summarySupport;
    @Mock private CashRegisterMapper mapper;
    @Mock private com.sni.bokaticowork.features.payment.service.support.CashEmailNotifier emailNotifier;
    @Mock private com.sni.bokaticowork.features.payment.repository.CashMovementAttachmentRepository attachmentRepository;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks private CashRegisterServiceImpl service;

    private CashRegister mobileMoneyRegister;

    @BeforeEach
    void setUp() {
        mobileMoneyRegister = CashRegister.builder()
                .id(20L)
                .registerCode("CSR-AUTO-MOBILE-MONEY")
                .name("Caisse automatique - Mobile money")
                .active(true)
                .cashControlEnabled(true)
                .systemManaged(true)
                .restrictedToMethod(PaymentMethod.MOBILE_MONEY)
                .build();
        when(registerRepository.findByRegisterCode("CSR-AUTO-MOBILE-MONEY")).thenReturn(Optional.of(mobileMoneyRegister));
        when(movementRepository.existsByReferenceTypeAndReferenceCode(anyString(), anyString())).thenReturn(false);
        when(movementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionRepository.save(any(CashSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sequenceGenerator.next(anyString())).thenReturn("SEQ-0001");
        when(summarySupport.expectedClosingAmount(any())).thenReturn(BigDecimal.ZERO);
    }

    private static PaymentTransaction mobileMoney(PaymentMethod method) {
        return PaymentTransaction.builder()
                .transactionNumber("TXN-MM-1")
                .paymentMethod(method)
                .status(PaymentTransactionStatus.SUCCEEDED)
                .amount(new BigDecimal("25000"))
                .currency("XAF")
                .build();
    }

    @Test
    @DisplayName("Aucune session ouverte sur la caisse mobile money : elle est ouverte et le paiement y entre")
    void opensTheAutomaticSessionWhenNoneIsOpen() {
        when(sessionRepository.findFirstByCashRegisterIdAndStatusForUpdate(20L, "OPEN")).thenReturn(Optional.empty());

        service.recordAutomaticPayment(mobileMoney(PaymentMethod.MOBILE_MONEY));

        ArgumentCaptor<CashSession> opened = ArgumentCaptor.forClass(CashSession.class);
        verify(sessionRepository).save(opened.capture());
        assertThat(opened.getValue().getCashRegister()).isSameAs(mobileMoneyRegister);
        assertThat(opened.getValue().getStatus()).isEqualTo(CashSessionStatus.OPEN);
        assertThat(opened.getValue().getOpenedBy()).isEqualTo("SYSTEM_MOBILE_MONEY");

        ArgumentCaptor<CashMovement> movement = ArgumentCaptor.forClass(CashMovement.class);
        verify(movementRepository).save(movement.capture());
        assertThat(movement.getValue().getCashSession()).isSameAs(opened.getValue());
        assertThat(movement.getValue().getAmount()).isEqualByComparingTo("25000");
        // La caisse est verrouillee avant de chercher la session : deux confirmations simultanees
        // n'ouvrent pas deux sessions.
        verify(registerRepository).lockById(20L);
    }

    @Test
    @DisplayName("Une session est deja ouverte : le paiement y entre directement")
    void usesTheOpenAutomaticSession() {
        CashSession open = CashSession.builder().id(200L).sessionNumber("CSS-AUTO-1")
                .cashRegister(mobileMoneyRegister).status(CashSessionStatus.OPEN).openingAmount(BigDecimal.ZERO).build();
        when(sessionRepository.findFirstByCashRegisterIdAndStatusForUpdate(20L, "OPEN")).thenReturn(Optional.of(open));

        service.recordAutomaticPayment(mobileMoney(PaymentMethod.MOBILE_MONEY));

        verify(sessionRepository, never()).save(any(CashSession.class));
        ArgumentCaptor<CashMovement> movement = ArgumentCaptor.forClass(CashMovement.class);
        verify(movementRepository).save(movement.capture());
        assertThat(movement.getValue().getCashSession()).isSameAs(open);
    }

    @Test
    @DisplayName("Mobile money saisi au guichet (hors plateforme) : jamais dans la caisse automatique")
    void offlineMobileMoneyStaysOutOfTheAutomaticRegister() {
        service.recordAutomaticPayment(mobileMoney(PaymentMethod.MOBILE_MONEY_OFFLINE));

        verify(movementRepository, never()).save(any());
        verify(sessionRepository, never()).save(any(CashSession.class));
    }

    @Test
    @DisplayName("Rattacher un paiement a une session : ouverte et ordinaire seulement")
    void counterSessionMustBeOpenAndOrdinary() {
        CashRegister counter = CashRegister.builder().id(11L).registerCode("CSR-ACCUEIL").systemManaged(false).build();
        CashSession open = CashSession.builder().sessionNumber("CSS-1").cashRegister(counter).status(CashSessionStatus.OPEN).build();
        CashSession closed = CashSession.builder().sessionNumber("CSS-2").cashRegister(counter).status(CashSessionStatus.CLOSED).build();
        CashSession automatic = CashSession.builder().sessionNumber("CSS-3").cashRegister(mobileMoneyRegister).status(CashSessionStatus.OPEN).build();
        when(sessionRepository.findBySessionNumber(eq("CSS-1"))).thenReturn(Optional.of(open));
        when(sessionRepository.findBySessionNumber(eq("CSS-2"))).thenReturn(Optional.of(closed));
        when(sessionRepository.findBySessionNumber(eq("CSS-3"))).thenReturn(Optional.of(automatic));

        assertThatCode(() -> service.assertCounterSessionOpen("CSS-1")).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.assertCounterSessionOpen("CSS-2")).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("pas ouverte");
        assertThatThrownBy(() -> service.assertCounterSessionOpen("CSS-3")).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("CSR-AUTO-MOBILE-MONEY");
    }
}
