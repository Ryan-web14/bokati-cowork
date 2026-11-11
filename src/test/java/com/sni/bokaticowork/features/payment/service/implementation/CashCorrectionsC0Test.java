package com.sni.bokaticowork.features.payment.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.dto.request.ApproveCashVarianceRequest;
import com.sni.bokaticowork.features.payment.dto.request.CreateCashMovementRequest;
import com.sni.bokaticowork.features.payment.dto.request.CreateCashVoucherRequest;
import com.sni.bokaticowork.features.payment.dto.request.OpenCashSessionRequest;
import com.sni.bokaticowork.features.payment.enums.CashDocumentType;
import com.sni.bokaticowork.features.payment.enums.CashFlowDirection;
import com.sni.bokaticowork.features.payment.enums.CashMovementStatus;
import com.sni.bokaticowork.features.payment.enums.CashMovementType;
import com.sni.bokaticowork.features.payment.enums.CashSessionStatus;
import com.sni.bokaticowork.features.payment.mapper.interfaces.CashRegisterMapper;
import com.sni.bokaticowork.features.payment.model.CashMovement;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.repository.CashMovementRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterRepository;
import com.sni.bokaticowork.features.payment.repository.CashSessionRepository;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import com.sni.bokaticowork.features.payment.service.support.CashEmailNotifier;
import com.sni.bokaticowork.features.payment.service.support.CashFlowRules;
import com.sni.bokaticowork.features.payment.service.support.CashSessionSummarySupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lot C0 du plan finance : correctifs des caisses. */
@ExtendWith(MockitoExtension.class)
class CashCorrectionsC0Test {

    @Mock private CashRegisterRepository registerRepository;
    @Mock private CashSessionRepository sessionRepository;
    @Mock private CashMovementRepository movementRepository;
    @Mock private SequenceGeneratorFacade sequenceGenerator;
    @Mock private CashSessionSummarySupport summarySupport;
    @Mock private CashRegisterMapper mapper;
    @Mock private CashEmailNotifier emailNotifier;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Spy private CashActorResolver actors = new CashActorResolver();

    @InjectMocks private CashRegisterServiceImpl service;

    /** Mouvements enregistres pendant le test, dans l'ordre, avec un identifiant pose a l'enregistrement. */
    private final List<CashMovement> stored = new ArrayList<>();
    private final AtomicLong ids = new AtomicLong();

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    private void loggedInAs(String email) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @Test
    @DisplayName("D1 : l'ouverture est au nom de l'utilisateur connecte, jamais de celui envoye")
    void openingUsesTheAuthenticatedUser() {
        loggedInAs("caissier@elleaose.com");
        CashRegister register = register();
        when(registerRepository.findByRegisterCode("CSR-1")).thenReturn(Optional.of(register));
        when(sessionRepository.findBlockingSessionByCashRegisterId(any())).thenReturn(Optional.empty());
        when(sessionRepository.findBlockingSessionByOpenedBy(anyString())).thenReturn(Optional.empty());
        when(sequenceGenerator.next(anyString())).thenReturn("CSS-1");
        when(sessionRepository.save(any(CashSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.openSession(new OpenCashSessionRequest("CSR-1", "quelquun.dautre@elleaose.com", BigDecimal.ZERO));

        ArgumentCaptor<CashSession> saved = ArgumentCaptor.forClass(CashSession.class);
        verify(sessionRepository).save(saved.capture());
        assertThat(saved.getValue().getOpenedBy()).isEqualTo("caissier@elleaose.com");
        verify(sessionRepository).findBlockingSessionByOpenedBy("caissier@elleaose.com");
    }

    @Test
    @DisplayName("D3 : le caissier ne valide pas l'ecart de sa propre session")
    void cashierCannotApproveOwnVariance() {
        loggedInAs("caissier@elleaose.com");
        CashSession session = reviewSession();
        when(sessionRepository.findBySessionNumber("CSS-1")).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.approveVariance("CSS-1", new ApproveCashVarianceRequest("superviseur@elleaose.com", null)))
                .isInstanceOf(ForbiddenException.class);
        assertThat(session.getStatus()).isEqualTo(CashSessionStatus.CLOSING_REVIEW);
        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("D3 : une autre personne valide l'ecart, et c'est elle qui est inscrite")
    void anotherPersonApprovesTheVariance() {
        loggedInAs("superviseur@elleaose.com");
        CashSession session = reviewSession();
        when(sessionRepository.findBySessionNumber("CSS-1")).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(CashSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.approveVariance("CSS-1", new ApproveCashVarianceRequest("caissier@elleaose.com", "ok"));

        assertThat(session.getStatus()).isEqualTo(CashSessionStatus.CLOSED);
        assertThat(session.getReviewedBy()).isEqualTo("superviseur@elleaose.com");
    }

    @Test
    @DisplayName("D4 : fonds de 10 000 puis encaissement de 5 000, le solde courant vaut 15 000")
    void runningBalanceCountsTheOpeningFloatOnce() {
        CashSession session = openSession(new BigDecimal("10000"));
        storeMovements();
        stored.add(movement(session, CashMovementType.OPENING_FLOAT, "10000", null, CashMovementStatus.CONFIRMED));
        when(sessionRepository.findBySessionNumber("CSS-1")).thenReturn(Optional.of(session));
        when(sequenceGenerator.next("cash_movement")).thenReturn("CSM-2");

        service.createEntryVoucher("CSS-1", new CreateCashVoucherRequest(new BigDecimal("5000"), "BE-1", null, null, null,
                null, null, null, "Vente de fournitures", "caissier@elleaose.com", CashDocumentType.ENTRY_VOUCHER, null));

        assertThat(stored.get(1).getRunningBalance()).isEqualByComparingTo("15000");
    }

    @Test
    @DisplayName("D5 : un ajustement sans sens est refuse")
    void adjustmentRequiresADirection() {
        CashSession session = openSession(BigDecimal.ZERO);
        when(sessionRepository.findBySessionNumber("CSS-1")).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.createMovement("CSS-1", adjustment(null)))
                .isInstanceOf(BadRequestException.class);
        verify(movementRepository, never()).save(any());
    }

    @Test
    @DisplayName("D5 : un ajustement en sortie baisse le solde courant")
    void outgoingAdjustmentLowersTheBalance() {
        CashSession session = openSession(new BigDecimal("10000"));
        storeMovements();
        stored.add(movement(session, CashMovementType.OPENING_FLOAT, "10000", null, CashMovementStatus.CONFIRMED));
        when(sessionRepository.findBySessionNumber("CSS-1")).thenReturn(Optional.of(session));
        when(sequenceGenerator.next("cash_movement")).thenReturn("CSM-2");

        service.createMovement("CSS-1", adjustment(CashFlowDirection.OUT));

        CashMovement adjustment = stored.get(1);
        assertThat(adjustment.getDirection()).isEqualTo(CashFlowDirection.OUT);
        assertThat(adjustment.getRunningBalance()).isEqualByComparingTo("8000");
    }

    @Test
    @DisplayName("D6 : un mouvement en attente ou annule ne compte pas, un contre-passe compte")
    void onlyConfirmedAndReversedMovementsCount() {
        CashSession session = openSession(BigDecimal.ZERO);
        assertThat(CashFlowRules.signedAmount(movement(session, CashMovementType.CASH_OUT, "500", null, CashMovementStatus.PENDING)))
                .isEqualByComparingTo("0");
        assertThat(CashFlowRules.signedAmount(movement(session, CashMovementType.CASH_OUT, "500", null, CashMovementStatus.CANCELLED)))
                .isEqualByComparingTo("0");
        assertThat(CashFlowRules.signedAmount(movement(session, CashMovementType.CASH_OUT, "500", null, CashMovementStatus.REVERSED)))
                .isEqualByComparingTo("-500");
        assertThat(CashFlowRules.signedAmount(movement(session, CashMovementType.CLOSING_COUNT, "500", null, CashMovementStatus.CONFIRMED)))
                .isEqualByComparingTo("0");
        assertThat(CashFlowRules.signedAmount(movement(session, CashMovementType.ADJUSTMENT, "500", null, CashMovementStatus.CONFIRMED)))
                .as("un ajustement ancien, sans sens, reste une entree comme avant")
                .isEqualByComparingTo("500");
    }

    // ---- outils ----

    private void storeMovements() {
        when(movementRepository.save(any(CashMovement.class))).thenAnswer(invocation -> {
            CashMovement movement = invocation.getArgument(0);
            if (movement.getId() == null) {
                movement.setId(ids.incrementAndGet() + 100);
                stored.add(movement);
            }
            return movement;
        });
        lenient().when(movementRepository.findByCashSession_IdOrderByCreatedAtAsc(anyLong()))
                .thenAnswer(invocation -> List.copyOf(stored));
    }

    private CreateCashMovementRequest adjustment(CashFlowDirection direction) {
        return new CreateCashMovementRequest(CashMovementType.ADJUSTMENT, new BigDecimal("2000"), null, null, null, null, null,
                null, null, null, "Erreur de saisie sur un bon", "caissier@elleaose.com", null, null, null, null, null,
                null, null, null, null, direction);
    }

    private CashRegister register() {
        return CashRegister.builder()
                .id(1L)
                .registerCode("CSR-1")
                .name("Caisse accueil")
                .active(true)
                .cashControlEnabled(true)
                .businessEntityCode("BIZ-1")
                .build();
    }

    private CashSession openSession(BigDecimal openingAmount) {
        return CashSession.builder()
                .id(10L)
                .sessionNumber("CSS-1")
                .cashRegister(register())
                .status(CashSessionStatus.OPEN)
                .openedBy("caissier@elleaose.com")
                .openingAmount(openingAmount)
                .build();
    }

    private CashSession reviewSession() {
        CashSession session = openSession(BigDecimal.ZERO);
        session.setStatus(CashSessionStatus.CLOSING_REVIEW);
        session.setClosedBy("caissier@elleaose.com");
        session.setVarianceAmount(new BigDecimal("-1000"));
        return session;
    }

    private CashMovement movement(CashSession session, CashMovementType type, String amount,
                                  CashFlowDirection direction, CashMovementStatus status) {
        return CashMovement.builder()
                .id(ids.incrementAndGet())
                .cashSession(session)
                .movementType(type)
                .amount(new BigDecimal(amount))
                .direction(direction)
                .status(status)
                .build();
    }
}
