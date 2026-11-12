package com.sni.bokaticowork.features.payment.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.dto.request.CreateCashVoucherRequest;
import com.sni.bokaticowork.features.payment.enums.CashCapBehavior;
import com.sni.bokaticowork.features.payment.enums.CashDocumentType;
import com.sni.bokaticowork.features.payment.enums.CashMovementStatus;
import com.sni.bokaticowork.features.payment.enums.CashMovementType;
import com.sni.bokaticowork.features.payment.enums.CashRegisterStatus;
import com.sni.bokaticowork.features.payment.enums.CashRegisterType;
import com.sni.bokaticowork.features.payment.enums.CashSessionStatus;
import com.sni.bokaticowork.features.payment.mapper.interfaces.CashRegisterMapper;
import com.sni.bokaticowork.features.payment.model.CashMovement;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.repository.CashMovementRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterRepository;
import com.sni.bokaticowork.features.payment.repository.CashSessionRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.CashSessionLifecycleService;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import com.sni.bokaticowork.features.payment.service.support.CashEmailNotifier;
import com.sni.bokaticowork.features.payment.service.support.CashRegisterAccessGuard;
import com.sni.bokaticowork.features.payment.service.support.CashSessionSummarySupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lot C4 : plafonds appliques et reimpressions tracees. */
@ExtendWith(MockitoExtension.class)
class CashControlsC4Test {

    @Mock private CashRegisterRepository registerRepository;
    @Mock private CashSessionRepository sessionRepository;
    @Mock private CashMovementRepository movementRepository;
    @Mock private SequenceGeneratorFacade sequenceGenerator;
    @Mock private CashSessionSummarySupport summarySupport;
    @Mock private CashRegisterMapper mapper;
    @Mock private CashEmailNotifier emailNotifier;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private CashRegisterAccessGuard accessGuard;
    @Mock private CashRegisterAdminServiceImpl registerAdmin;
    @Mock private CashSessionLifecycleService lifecycle;
    @Spy private CashActorResolver actors = new CashActorResolver();

    @InjectMocks private CashRegisterServiceImpl service;

    private CashRegister register;
    private CashSession session;
    private final List<CashMovement> stored = new ArrayList<>();

    @BeforeEach
    void setUp() {
        register = CashRegister.builder().id(1L).registerCode("CSR-1").name("Accueil").registerType(CashRegisterType.COUNTER)
                .status(CashRegisterStatus.ACTIVE).active(true).cashControlEnabled(true).systemManaged(false).build();
        session = CashSession.builder().id(10L).sessionNumber("CSS-1").cashRegister(register).status(CashSessionStatus.OPEN)
                .openedBy("caissier@elleaose.com").openingAmount(BigDecimal.ZERO).build();
        lenient().when(sessionRepository.findBySessionNumber("CSS-1")).thenReturn(Optional.of(session));
        lenient().when(sequenceGenerator.next(anyString())).thenReturn("CSM-1");
        lenient().when(movementRepository.save(any(CashMovement.class))).thenAnswer(invocation -> {
            CashMovement movement = invocation.getArgument(0);
            if (!stored.contains(movement)) {
                stored.add(movement);
            }
            return movement;
        });
        lenient().when(movementRepository.findByCashSession_IdOrderByCreatedAtAsc(anyLong())).thenReturn(List.of());
    }

    private CreateCashVoucherRequest voucher(String amount) {
        return new CreateCashVoucherRequest(new BigDecimal(amount), "B-1", null, null, null, null, null, null,
                "Operation", "caissier@elleaose.com", null, null);
    }

    @Test
    @DisplayName("Plafond de detention en blocage : l'encaissement en especes qui le depasse est refuse")
    void detentionCapBlocks() {
        register.setMaxCashAmount(new BigDecimal("100000"));
        register.setCapBehavior(CashCapBehavior.BLOCK);
        when(summarySupport.expectedClosingAmount(session)).thenReturn(new BigDecimal("95000"));

        assertThatThrownBy(() -> service.createEntryVoucher("CSS-1", voucher("10000")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("Plafond de detention");
        verify(movementRepository, never()).save(any());
    }

    @Test
    @DisplayName("Plafond de detention en alerte : l'encaissement passe, le responsable est prevenu")
    void detentionCapAlerts() {
        register.setMaxCashAmount(new BigDecimal("100000"));
        register.setCapBehavior(CashCapBehavior.ALERT);
        when(summarySupport.expectedClosingAmount(session)).thenReturn(new BigDecimal("95000"));

        service.createEntryVoucher("CSS-1", voucher("10000"));

        verify(emailNotifier).notifyRegisterManager(any(), anyString(), anyString(), anyString(), anyString());
        assertThat(stored).isNotEmpty();
    }

    @Test
    @DisplayName("Sortie au-dela du plafond par operation du caissier : en attente de visa")
    void operationCapNeedsVisa() {
        register.setMaxOutPerOperation(new BigDecimal("50000"));

        service.createExitVoucher("CSS-1", voucher("60000"));

        assertThat(stored.get(0).getStatus()).isEqualTo(CashMovementStatus.PENDING);
        assertThat(stored.get(0).getExpiresAt()).isNotNull();
    }

    @Test
    @DisplayName("Sortie sous les plafonds : confirmee d'office")
    void smallOutflowIsConfirmed() {
        register.setMaxOutPerOperation(new BigDecimal("50000"));
        register.setDoubleVisaThreshold(new BigDecimal("50000"));

        service.createExitVoucher("CSS-1", voucher("20000"));

        assertThat(stored.get(0).getStatus()).isEqualTo(CashMovementStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Reimpression : la premiere date reste, le compteur avance (duplicata)")
    void reprintIsTracked() {
        CashMovement movement = CashMovement.builder().id(5L).movementNumber("CSM-5").cashSession(session)
                .movementType(CashMovementType.CASH_OUT).amount(BigDecimal.TEN).documentType(CashDocumentType.EXIT_VOUCHER).build();
        when(movementRepository.findByMovementNumber("CSM-5")).thenReturn(Optional.of(movement));

        service.markMovementPrinted("CSM-5");
        var first = movement.getPrintedAt();
        service.markMovementPrinted("CSM-5");

        assertThat(movement.getPrintCount()).isEqualTo(2);
        assertThat(movement.getPrintedAt()).isEqualTo(first);
    }
}
