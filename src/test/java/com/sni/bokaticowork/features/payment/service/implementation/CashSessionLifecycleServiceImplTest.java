package com.sni.bokaticowork.features.payment.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.dto.request.CashCountRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashHandoverAnswerRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashHandoverRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashReasonRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashVarianceDecisionRequest;
import com.sni.bokaticowork.features.payment.dto.request.ResumeCashSessionRequest;
import com.sni.bokaticowork.features.payment.dto.response.CashSessionResponse;
import com.sni.bokaticowork.features.payment.enums.CashAssignmentQuality;
import com.sni.bokaticowork.features.payment.enums.CashAssignmentRole;
import com.sni.bokaticowork.features.payment.enums.CashCountMoment;
import com.sni.bokaticowork.features.payment.enums.CashHandoverStatus;
import com.sni.bokaticowork.features.payment.enums.CashMovementType;
import com.sni.bokaticowork.features.payment.enums.CashRegisterStatus;
import com.sni.bokaticowork.features.payment.enums.CashRegisterType;
import com.sni.bokaticowork.features.payment.enums.CashSessionStatus;
import com.sni.bokaticowork.features.payment.enums.CashVarianceDecision;
import com.sni.bokaticowork.features.payment.enums.CashVarianceKind;
import com.sni.bokaticowork.features.payment.enums.CashVarianceStatus;
import com.sni.bokaticowork.features.payment.mapper.interfaces.CashRegisterMapper;
import com.sni.bokaticowork.features.payment.model.CashCount;
import com.sni.bokaticowork.features.payment.model.CashHandover;
import com.sni.bokaticowork.features.payment.model.CashMovement;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashRegisterAssignment;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.model.CashVarianceReview;
import com.sni.bokaticowork.features.payment.repository.CashCountRepository;
import com.sni.bokaticowork.features.payment.repository.CashDelegationRepository;
import com.sni.bokaticowork.features.payment.repository.CashDenominationRepository;
import com.sni.bokaticowork.features.payment.repository.CashHandoverRepository;
import com.sni.bokaticowork.features.payment.repository.CashMovementRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterRepository;
import com.sni.bokaticowork.features.payment.repository.CashSessionRepository;
import com.sni.bokaticowork.features.payment.repository.CashVarianceReviewRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.CashRegisterService;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import com.sni.bokaticowork.features.payment.service.support.CashCountRecorder;
import com.sni.bokaticowork.features.payment.service.support.CashEmailNotifier;
import com.sni.bokaticowork.features.payment.service.support.CashReferenceLists;
import com.sni.bokaticowork.features.payment.service.support.CashRegisterAccessGuard;
import com.sni.bokaticowork.features.payment.service.support.CashSessionSummarySupport;
import com.sni.bokaticowork.security.admin.user.model.Users;
import com.sni.bokaticowork.security.admin.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lot C2 : cloture comptee, tolerance, double comptage, decision, cloture d'office, passation. */
@ExtendWith(MockitoExtension.class)
class CashSessionLifecycleServiceImplTest {

    private static final String CASHIER = "caissier@elleaose.com";
    private static final String SUPERVISOR = "superviseur@elleaose.com";

    @Mock private CashSessionRepository sessionRepository;
    @Mock private CashRegisterRepository registerRepository;
    @Mock private CashMovementRepository movementRepository;
    @Mock private CashCountRepository countRepository;
    @Mock private CashDenominationRepository denominationRepository;
    @Mock private CashVarianceReviewRepository reviewRepository;
    @Mock private CashHandoverRepository handoverRepository;
    @Mock private CashDelegationRepository delegationRepository;
    @Mock private CashCountRecorder countRecorder;
    @Mock private CashSessionSummarySupport summarySupport;
    @Mock private CashRegisterAccessGuard accessGuard;
    @Mock private CashReferenceLists referenceLists;
    @Spy private CashActorResolver actors = new CashActorResolver();
    @Mock private CashEmailNotifier emailNotifier;
    @Mock private SequenceGeneratorFacade sequenceGenerator;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private CashRegisterMapper mapper;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private CashRegisterService cashRegisterService;

    @InjectMocks private CashSessionLifecycleServiceImpl service;

    private CashRegister register;
    private CashSession session;
    private final List<CashVarianceReview> reviews = new ArrayList<>();
    private final List<CashCount> counts = new ArrayList<>();
    private final AtomicInteger sequence = new AtomicInteger();

    @BeforeEach
    void setUp() {
        register = CashRegister.builder().id(1L).registerCode("CSR-1").name("Accueil").registerType(CashRegisterType.COUNTER)
                .status(CashRegisterStatus.ACTIVE).active(true).blindClose(true).countLinesRequired(false)
                .countTolerance(new BigDecimal("100")).doubleCountThreshold(new BigDecimal("10000")).build();
        session = CashSession.builder().id(10L).sessionNumber("CSS-1").cashRegister(register).status(CashSessionStatus.OPEN)
                .openedBy(CASHIER).openingAmount(new BigDecimal("10000")).build();
        lenient().when(sessionRepository.findBySessionNumber("CSS-1")).thenReturn(Optional.of(session));
        lenient().when(summarySupport.expectedClosingAmount(session)).thenReturn(new BigDecimal("50000"));
        lenient().when(sequenceGenerator.next(anyString())).thenAnswer(invocation -> invocation.getArgument(0) + "-" + sequence.incrementAndGet());
        lenient().when(reviewRepository.save(any(CashVarianceReview.class))).thenAnswer(invocation -> {
            CashVarianceReview review = invocation.getArgument(0);
            if (!reviews.contains(review)) {
                reviews.add(review);
            }
            return review;
        });
        lenient().when(reviewRepository.findBySessionNumberOrderByCreatedAtAsc("CSS-1")).thenAnswer(invocation -> List.copyOf(reviews));
        lenient().when(countRepository.findBySessionNumberOrderByCreatedAtAsc("CSS-1")).thenAnswer(invocation -> List.copyOf(counts));
        lenient().when(countRecorder.record(any(), any(), any(), any(), anyString(), any(), any(), any(), any())).thenAnswer(invocation -> {
            CashCountRequest request = invocation.getArgument(3);
            BigDecimal expected = invocation.getArgument(5);
            CashCount count = CashCount.builder().countNumber("PVB-" + sequence.incrementAndGet()).registerCode("CSR-1")
                    .sessionNumber(invocation.getArgument(1)).moment(invocation.getArgument(2)).currency("XAF")
                    .total(request.totalAmount()).withLines(false).expectedAmount(expected)
                    .variance(expected == null ? null : request.totalAmount().subtract(expected))
                    .countedBy(invocation.getArgument(4)).witness(invocation.getArgument(8)).build();
            counts.add(count);
            return count;
        });
        lenient().when(referenceLists.describe(eq(CashSessionLifecycleServiceImpl.VARIANCE_REASON), anyString(), any()))
                .thenReturn("Erreur de rendu de monnaie");
        lenient().when(delegationRepository.findByCancelledAtIsNullOrderByStartsOnAsc()).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loggedInAs(String email, String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email, null,
                java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
    }

    private static CashCountRequest total(String amount, String reasonCode) {
        return new CashCountRequest(null, null, new BigDecimal(amount), null, null, null, reasonCode, null, null);
    }

    @Test
    @DisplayName("Comptage egal a l'attendu : session close, dernier comptage de la caisse mis a jour")
    void exactCountCloses() {
        loggedInAs(CASHIER);
        service.startClosing("CSS-1");

        service.submitClosingCount("CSS-1", total("50000", null));

        assertThat(session.getStatus()).isEqualTo(CashSessionStatus.CLOSED);
        assertThat(register.getLastCountAmount()).isEqualByComparingTo("50000");
        assertThat(reviews).isEmpty();
    }

    @Test
    @DisplayName("Ecart dans la tolerance : close, ecart comptabilise sans revue")
    void toleratedVarianceCloses() {
        loggedInAs(CASHIER);
        service.submitClosingCount("CSS-1", total("49950", null));

        assertThat(session.getStatus()).isEqualTo(CashSessionStatus.CLOSED);
        assertThat(session.getVarianceDecision()).isEqualTo(CashVarianceDecision.TOLERATED);
        assertThat(reviews).singleElement().satisfies(review -> assertThat(review.getStatus()).isEqualTo(CashVarianceStatus.TOLERATED));
    }

    @Test
    @DisplayName("Ecart hors tolerance : motif exige, puis session en revue")
    void varianceBeyondToleranceNeedsAReason() {
        loggedInAs(CASHIER);
        assertThatThrownBy(() -> service.submitClosingCount("CSS-1", total("48000", null)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("motif");

        session.setStatus(CashSessionStatus.OPEN);
        service.submitClosingCount("CSS-1", total("48000", "CHANGE_ERROR"));

        assertThat(session.getStatus()).isEqualTo(CashSessionStatus.CLOSING_REVIEW);
        assertThat(reviews).anySatisfy(review -> {
            assertThat(review.getStatus()).isEqualTo(CashVarianceStatus.PENDING);
            assertThat(review.getAmount()).isEqualByComparingTo("-2000");
        });
    }

    @Test
    @DisplayName("Au-dela du seuil de double comptage : recomptage par une autre personne")
    void bigVarianceNeedsASecondCounter() {
        loggedInAs(CASHIER);
        service.submitClosingCount("CSS-1", total("30000", null));
        assertThat(session.getStatus()).isEqualTo(CashSessionStatus.RECOUNT);

        assertThatThrownBy(() -> service.submitClosingCount("CSS-1", total("30000", "UNEXPLAINED")))
                .isInstanceOf(ForbiddenException.class);

        loggedInAs(SUPERVISOR);
        service.submitClosingCount("CSS-1", total("50000", null));
        assertThat(session.getStatus()).isEqualTo(CashSessionStatus.CLOSED);
    }

    @Test
    @DisplayName("Le caissier ne decide pas de son ecart ; une autre personne le fait, charge par defaut pour un manquant")
    void decisionByAnotherPerson() {
        loggedInAs(CASHIER);
        service.submitClosingCount("CSS-1", total("48000", "CHANGE_ERROR"));

        assertThatThrownBy(() -> service.decideSessionVariance("CSS-1", null)).isInstanceOf(ForbiddenException.class);

        loggedInAs(SUPERVISOR);
        service.decideSessionVariance("CSS-1", new CashVarianceDecisionRequest(null, "Rendu de monnaie", null));

        assertThat(session.getStatus()).isEqualTo(CashSessionStatus.CLOSED);
        assertThat(session.getVarianceDecision()).isEqualTo(CashVarianceDecision.EXPENSE);
        assertThat(session.getReviewedBy()).isEqualTo(SUPERVISOR);
    }

    @Test
    @DisplayName("Cloture d'office : temoin exige ; l'ecart n'est decide qu'apres la contestation ou le delai")
    void forcedCloseWithContest() {
        loggedInAs(SUPERVISOR);
        assertThatThrownBy(() -> service.forceClose("CSS-1", total("47000", null), null))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("temoin");

        CashCountRequest counted = new CashCountRequest(null, null, new BigDecimal("47000"), null, "temoin@elleaose.com", null, null, null, null);
        service.forceClose("CSS-1", counted, new CashReasonRequest(null, null, "Caissier malade"));
        assertThat(session.getStatus()).isEqualTo(CashSessionStatus.CLOSING_REVIEW);
        assertThat(session.getForcedClose()).isTrue();

        loggedInAs("autre.superviseur@elleaose.com");
        assertThatThrownBy(() -> service.decideSessionVariance("CSS-1", null))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("contester");

        loggedInAs(CASHIER);
        service.contest("CSS-1", new CashReasonRequest(null, "Le fonds contenait un billet de 2 000 de plus", null));

        loggedInAs("autre.superviseur@elleaose.com");
        service.decideSessionVariance("CSS-1", new CashVarianceDecisionRequest(CashVarianceDecision.INVESTIGATION, null, null));
        assertThat(reviews).anySatisfy(review -> {
            assertThat(review.getKind()).isEqualTo(CashVarianceKind.FORCED_CLOSE);
            assertThat(review.getReviewDueAt()).isNotNull();
        });
    }

    @Test
    @DisplayName("Une session avec des operations ne s'annule pas")
    void sessionWithOperationsCannotBeCancelled() {
        loggedInAs(CASHIER);
        when(movementRepository.findByCashSession_IdOrderByCreatedAtAsc(10L)).thenReturn(List.of(
                CashMovement.builder().movementType(CashMovementType.OPENING_FLOAT).amount(BigDecimal.TEN).build(),
                CashMovement.builder().movementType(CashMovementType.PAYMENT).amount(BigDecimal.TEN).build()));

        assertThatThrownBy(() -> service.cancel("CSS-1", new CashReasonRequest("OPENED_BY_MISTAKE", null, null)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Pause puis reprise : le mot de passe est redemande")
    void resumeNeedsThePassword() {
        loggedInAs(CASHIER);
        service.pause("CSS-1");
        assertThat(session.getStatus()).isEqualTo(CashSessionStatus.PAUSED);

        Users user = new Users();
        user.setPasswordHash("hash");
        when(userRepository.findByEmailIgnoreCase(CASHIER)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("faux", "hash")).thenReturn(false);
        when(passwordEncoder.matches("bon", "hash")).thenReturn(true);

        assertThatThrownBy(() -> service.resume("CSS-1", new ResumeCashSessionRequest("faux"))).isInstanceOf(ForbiddenException.class);
        service.resume("CSS-1", new ResumeCashSessionRequest("bon"));
        assertThat(session.getStatus()).isEqualTo(CashSessionStatus.OPEN);
    }

    @Test
    @DisplayName("Passation acceptee : la session du sortant se clot, celle de l'entrant s'ouvre avec le meme montant")
    void acceptedHandover() {
        loggedInAs(CASHIER);
        when(accessGuard.effectiveAssignment(eq("CSR-1"), eq("releve@elleaose.com"), any())).thenReturn(Optional.of(
                CashRegisterAssignment.builder().registerCode("CSR-1").userEmail("releve@elleaose.com")
                        .role(CashAssignmentRole.CASHIER).quality(CashAssignmentQuality.BACKUP).build()));
        List<CashHandover> handovers = new ArrayList<>();
        when(handoverRepository.save(any(CashHandover.class))).thenAnswer(invocation -> {
            CashHandover handover = invocation.getArgument(0);
            if (!handovers.contains(handover)) {
                handovers.add(handover);
            }
            return handover;
        });
        when(handoverRepository.findByHandoverNumber(anyString())).thenAnswer(invocation -> handovers.stream()
                .filter(handover -> handover.getHandoverNumber().equals(invocation.getArgument(0))).findFirst());
        when(cashRegisterService.openSessionFromHandover(eq("CSR-1"), eq("releve@elleaose.com"), any(), anyString(), any()))
                .thenReturn(new CashSessionResponse("CSS-2", "CSR-1", CashSessionStatus.OPEN, "releve@elleaose.com", null, null,
                        new BigDecimal("50000"), null, null, null, null, null, Instant.now(), null, null));

        var handover = service.startHandover("CSS-1", new CashHandoverRequest("releve@elleaose.com", total("50000", null)));
        assertThat(session.getStatus()).isEqualTo(CashSessionStatus.COUNTING);

        loggedInAs("releve@elleaose.com");
        var result = service.acceptHandover(handover.handoverNumber(), new CashHandoverAnswerRequest(null, null, null));

        assertThat(session.getStatus()).isEqualTo(CashSessionStatus.CLOSED);
        assertThat(result.session().sessionNumber()).isEqualTo("CSS-2");
        assertThat(handovers.get(0).getStatus()).isEqualTo(CashHandoverStatus.ACCEPTED);
        assertThat(handovers.get(0).getOutgoingSignedAt()).isNotNull();
        assertThat(handovers.get(0).getIncomingSignedAt()).isNotNull();
        verify(cashRegisterService).openSessionFromHandover(eq("CSR-1"), eq("releve@elleaose.com"), any(), anyString(), any());
    }

    @Test
    @DisplayName("Passation contestee : la session du sortant part en revue")
    void disputedHandover() {
        loggedInAs(CASHIER);
        when(accessGuard.effectiveAssignment(eq("CSR-1"), eq("releve@elleaose.com"), any())).thenReturn(Optional.of(
                CashRegisterAssignment.builder().registerCode("CSR-1").userEmail("releve@elleaose.com")
                        .role(CashAssignmentRole.CASHIER).quality(CashAssignmentQuality.BACKUP).build()));
        CashHandover[] stored = new CashHandover[1];
        when(handoverRepository.save(any(CashHandover.class))).thenAnswer(invocation -> stored[0] = invocation.getArgument(0));
        when(handoverRepository.findByHandoverNumber(anyString())).thenAnswer(invocation -> Optional.ofNullable(stored[0]));
        when(cashRegisterService.openSessionFromHandover(any(), any(), any(), any(), any()))
                .thenReturn(new CashSessionResponse("CSS-2", "CSR-1", CashSessionStatus.OPEN, "releve@elleaose.com", null, null,
                        new BigDecimal("49000"), null, null, null, null, null, Instant.now(), null, null));

        var handover = service.startHandover("CSS-1", new CashHandoverRequest("releve@elleaose.com", total("50000", null)));
        loggedInAs("releve@elleaose.com");
        service.disputeHandover(handover.handoverNumber(), new CashHandoverAnswerRequest(total("49000", null), "Il manque un billet de 1 000", null));

        assertThat(session.getStatus()).isEqualTo(CashSessionStatus.CLOSING_REVIEW);
        assertThat(stored[0].getStatus()).isEqualTo(CashHandoverStatus.DISPUTED);
        assertThat(reviews).anySatisfy(review -> assertThat(review.getKind()).isEqualTo(CashVarianceKind.HANDOVER));
    }

    @Test
    @DisplayName("Ecart d'ouverture : confirme avec un motif, signale, la session s'ouvre")
    void openingVarianceIsConfirmed() {
        loggedInAs(CASHIER);
        session.setStatus(CashSessionStatus.OPENING);
        session.setOpeningVariance(new BigDecimal("-1000"));

        assertThatThrownBy(() -> service.confirmOpening("CSS-1", null)).isInstanceOf(BadRequestException.class);
        service.confirmOpening("CSS-1", new CashReasonRequest("CHANGE_ERROR", null, null));

        assertThat(session.getStatus()).isEqualTo(CashSessionStatus.OPEN);
        assertThat(reviews).anySatisfy(review -> assertThat(review.getKind()).isEqualTo(CashVarianceKind.OPENING));
        verify(emailNotifier).notifySupervisor(anyString(), anyString(), eq("CSS-1"), anyString());
    }

    @Test
    @DisplayName("La tolerance en pourcentage s'ajoute a la tolerance en montant")
    void tolerancePercent() {
        CashRegister percent = CashRegister.builder().countTolerance(BigDecimal.ZERO).countTolerancePercent(new BigDecimal("1")).build();
        assertThat(CashSessionLifecycleServiceImpl.withinTolerance(percent, new BigDecimal("100000"), new BigDecimal("-900"))).isTrue();
        assertThat(CashSessionLifecycleServiceImpl.withinTolerance(percent, new BigDecimal("100000"), new BigDecimal("-1500"))).isFalse();
        verify(sessionRepository, never()).save(any());
    }
}
