package com.sni.bokaticowork.features.payment.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.dto.request.CashCountRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashDenominationRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashHandoverAnswerRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashHandoverRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashReasonRequest;
import com.sni.bokaticowork.features.payment.dto.request.CashVarianceDecisionRequest;
import com.sni.bokaticowork.features.payment.dto.request.ResumeCashSessionRequest;
import com.sni.bokaticowork.features.payment.dto.response.CashCountResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashDenominationResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashExpectedOpeningResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashHandoverResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashSessionResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashSessionStepResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashVarianceReviewResponse;
import com.sni.bokaticowork.features.payment.enums.CashCountMoment;
import com.sni.bokaticowork.features.payment.enums.CashHandoverStatus;
import com.sni.bokaticowork.features.payment.enums.CashMovementStatus;
import com.sni.bokaticowork.features.payment.enums.CashMovementType;
import com.sni.bokaticowork.features.payment.enums.CashSessionStatus;
import com.sni.bokaticowork.features.payment.enums.CashVarianceDecision;
import com.sni.bokaticowork.features.payment.enums.CashVarianceKind;
import com.sni.bokaticowork.features.payment.enums.CashVarianceStatus;
import com.sni.bokaticowork.features.payment.mapper.interfaces.CashRegisterMapper;
import com.sni.bokaticowork.features.payment.model.CashCount;
import com.sni.bokaticowork.features.payment.model.CashDelegation;
import com.sni.bokaticowork.features.payment.model.CashDenomination;
import com.sni.bokaticowork.features.payment.model.CashHandover;
import com.sni.bokaticowork.features.payment.model.CashMovement;
import com.sni.bokaticowork.features.payment.model.CashRegister;
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
import com.sni.bokaticowork.features.payment.service.interfaces.CashSessionLifecycleService;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import com.sni.bokaticowork.features.payment.service.support.CashCountRecorder;
import com.sni.bokaticowork.features.payment.service.support.CashEmailNotifier;
import com.sni.bokaticowork.features.payment.service.support.CashReferenceLists;
import com.sni.bokaticowork.features.payment.service.support.CashRegisterAccessGuard;
import com.sni.bokaticowork.features.payment.service.support.CashSessionClosedEvent;
import com.sni.bokaticowork.features.payment.service.support.CashSessionFinalizedEvent;
import com.sni.bokaticowork.features.payment.service.support.CashSessionSummarySupport;
import com.sni.bokaticowork.features.payment.enums.CashAssignmentRole;
import com.sni.bokaticowork.security.admin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cycle de vie des sessions de caisse et comptages (lot C2).
 *
 * <p>Regles tenues ici :</p>
 * <ul>
 *   <li>l'attendu de cloture n'est revele qu'une fois le comptage valide (cloture a l'aveugle) ;</li>
 *   <li>un ecart dans la tolerance de la caisse clot la session, comptabilise sans revue ; au-dela,
 *       un motif est exige et la session part en revue ;</li>
 *   <li>au-dela du seuil de double comptage, un second compteur different du premier recompte ;</li>
 *   <li>celui qui decide d'un ecart n'est jamais le caissier de la session ;</li>
 *   <li>une cloture d'office est comptee par un superviseur et un temoin, et le caissier peut la
 *       contester pendant un delai, pendant lequel l'ecart n'est pas impute.</li>
 * </ul>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CashSessionLifecycleServiceImpl implements CashSessionLifecycleService {

    static final String VARIANCE_REASON = "VARIANCE_REASON";
    static final String SESSION_CANCEL_REASON = "SESSION_CANCEL_REASON";
    private static final String SESSIONS_PATH = "/payments/cash-registers/sessions/";
    private static final String DECIDE = "CASH_VARIANCE:DECIDE";

    private final CashSessionRepository sessionRepository;
    private final CashRegisterRepository registerRepository;
    private final CashMovementRepository movementRepository;
    private final CashCountRepository countRepository;
    private final CashDenominationRepository denominationRepository;
    private final CashVarianceReviewRepository reviewRepository;
    private final CashHandoverRepository handoverRepository;
    private final CashDelegationRepository delegationRepository;
    private final CashCountRecorder countRecorder;
    private final CashSessionSummarySupport summarySupport;
    private final CashRegisterAccessGuard accessGuard;
    private final CashReferenceLists referenceLists;
    private final CashActorResolver actors;
    private final CashEmailNotifier emailNotifier;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final ApplicationEventPublisher eventPublisher;
    private final CashRegisterMapper mapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    @Lazy
    private final CashRegisterService cashRegisterService;
    @Lazy
    private final com.sni.bokaticowork.features.payment.service.interfaces.CashOperationsService operations;

    @Value("${bokati.cash.forced-close.contest-days:3}")
    private int contestDays = 3;

    @Value("${bokati.cash.variance.investigation-days:30}")
    private int investigationDays = 30;

    // ------------------------------------------------------------------ ouverture

    @Override
    @Transactional(readOnly = true)
    public CashExpectedOpeningResponse expectedOpening(String registerCode) {
        CashRegister register = registerRepository.findByRegisterCode(registerCode == null ? null : registerCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Caisse introuvable : " + registerCode));
        return new CashExpectedOpeningResponse(register.getRegisterCode(),
                register.getLastCountAmount() == null ? BigDecimal.ZERO : register.getLastCountAmount(),
                register.getLastCountAt(), register.getLastCountAt() == null, register.getStandardFloat(),
                register.getCountLinesRequired(), denominations(CashCountRecorder.DEFAULT_CURRENCY, false));
    }

    @Override
    public void recordOpeningCount(CashSession session, CashCountRequest count, BigDecimal expected) {
        if (count == null) {
            return;
        }
        countRecorder.record(session.getCashRegister(), session.getSessionNumber(), CashCountMoment.OPENING, count,
                session.getOpenedBy(), expected, null, null, null);
    }

    @Override
    public CashSessionStepResponse confirmOpening(String sessionNumber, CashReasonRequest request) {
        CashSession session = session(sessionNumber);
        requireStatus(session, CashSessionStatus.OPENING);
        String actor = actors.resolve(null);
        requireCashier(session, actor, "confirmer l'ouverture");
        BigDecimal variance = session.getOpeningVariance() == null ? BigDecimal.ZERO : session.getOpeningVariance();
        CashVarianceReview review = null;
        List<String> messages = new ArrayList<>();
        if (variance.signum() != 0) {
            String reason = varianceReason(request == null ? null : request.reasonCode(), request == null ? null : request.reasonText(),
                    "Indiquez le motif de l'ecart d'ouverture");
            session.setOpeningVarianceReason(reason);
            review = review(session, CashVarianceKind.OPENING, variance, null, reason, CashVarianceStatus.PENDING);
            notifyReviewers("Ecart d'ouverture a revoir · " + session.getSessionNumber(),
                    "La caisse " + session.getCashRegister().getName() + " a ete ouverte par " + session.getOpenedBy()
                            + " avec un fonds different du dernier comptage : ecart de " + plain(variance) + " XAF. Motif : " + reason + ".",
                    session.getSessionNumber());
            messages.add("Ecart d'ouverture enregistre et signale au responsable");
        }
        session.setStatus(CashSessionStatus.OPEN);
        sessionRepository.save(session);
        return step(session, null, toResponse(review), messages);
    }

    // ------------------------------------------------------- pause et suspension

    @Override
    public CashSessionStepResponse pause(String sessionNumber) {
        CashSession session = session(sessionNumber);
        requireStatus(session, CashSessionStatus.OPEN);
        requireCashier(session, actors.resolve(null), "mettre la session en pause");
        session.setStatus(CashSessionStatus.PAUSED);
        session.setPausedAt(Instant.now());
        sessionRepository.save(session);
        return step(session, null, null, List.of("Session en pause : aucune operation jusqu'a la reprise"));
    }

    @Override
    public CashSessionStepResponse resume(String sessionNumber, ResumeCashSessionRequest request) {
        CashSession session = session(sessionNumber);
        requireStatus(session, CashSessionStatus.PAUSED);
        String actor = actors.resolve(null);
        requireCashier(session, actor, "reprendre la session");
        checkPassword(actor, request == null ? null : request.password());
        Instant now = Instant.now();
        if (session.getPausedAt() != null) {
            long seconds = Duration.between(session.getPausedAt(), now).getSeconds();
            session.setPauseSeconds((session.getPauseSeconds() == null ? 0L : session.getPauseSeconds()) + Math.max(0, seconds));
        }
        session.setPausedAt(null);
        session.setStatus(CashSessionStatus.OPEN);
        sessionRepository.save(session);
        return step(session, null, null, List.of());
    }

    @Override
    public CashSessionStepResponse suspend(String sessionNumber, CashReasonRequest request) {
        CashSession session = session(sessionNumber);
        if (!EnumSet.of(CashSessionStatus.OPENING, CashSessionStatus.OPEN, CashSessionStatus.PAUSED).contains(session.getStatus())) {
            throw new BadRequestException("Seule une session ouverte ou en pause peut etre suspendue");
        }
        String actor = actors.resolve(null);
        requireNotCashier(session, actor, "suspendre sa propre session");
        String reason = referenceLists.describe(CashReferenceLists.SUSPENSION_REASON,
                request == null ? null : request.reasonCode(), request == null ? null : request.reasonText());
        applySuspension(session, actor, reason);
        emailNotifier.notifyUser(session.getOpenedBy(), "Session de caisse suspendue · " + session.getSessionNumber(),
                "Votre session " + session.getSessionNumber() + " a ete suspendue par " + actor + ". Motif : " + reason
                        + ". Aucune operation n'est possible jusqu'a la levee de la suspension.",
                session.getSessionNumber(), SESSIONS_PATH + session.getSessionNumber());
        return step(session, null, null, List.of());
    }

    @Override
    public void suspendBySystem(String sessionNumber, String reason) {
        CashSession session = session(sessionNumber);
        if (!EnumSet.of(CashSessionStatus.OPENING, CashSessionStatus.OPEN, CashSessionStatus.PAUSED).contains(session.getStatus())) {
            return;
        }
        applySuspension(session, "SYSTEM", reason);
        notifyReviewers("Session de caisse suspendue · " + session.getSessionNumber(),
                "La session " + session.getSessionNumber() + " de " + session.getOpenedBy() + " sur la caisse "
                        + session.getCashRegister().getName() + " a ete suspendue : " + reason
                        + ". Un superviseur doit la compter et la clore d'office.", session.getSessionNumber());
        emailNotifier.notifyUser(session.getOpenedBy(), "Session de caisse suspendue · " + session.getSessionNumber(),
                "Votre session " + session.getSessionNumber() + " a ete suspendue : " + reason + ".",
                session.getSessionNumber(), SESSIONS_PATH + session.getSessionNumber());
    }

    private void applySuspension(CashSession session, String actor, String reason) {
        session.setStatusBeforeSuspension(session.getStatus());
        if (session.getStatus() == CashSessionStatus.PAUSED && session.getPausedAt() != null) {
            long seconds = Duration.between(session.getPausedAt(), Instant.now()).getSeconds();
            session.setPauseSeconds((session.getPauseSeconds() == null ? 0L : session.getPauseSeconds()) + Math.max(0, seconds));
            session.setPausedAt(null);
        }
        session.setStatus(CashSessionStatus.SUSPENDED);
        session.setSuspendedAt(Instant.now());
        session.setSuspendedBy(actor);
        session.setSuspensionReason(reason);
        sessionRepository.save(session);
    }

    @Override
    public CashSessionStepResponse unsuspend(String sessionNumber) {
        CashSession session = session(sessionNumber);
        requireStatus(session, CashSessionStatus.SUSPENDED);
        String actor = actors.resolve(null);
        requireNotCashier(session, actor, "lever la suspension de sa propre session");
        session.setStatus(session.getStatusBeforeSuspension() == CashSessionStatus.OPENING ? CashSessionStatus.OPENING : CashSessionStatus.OPEN);
        session.setStatusBeforeSuspension(null);
        sessionRepository.save(session);
        emailNotifier.notifyUser(session.getOpenedBy(), "Suspension levee · " + session.getSessionNumber(),
                "La suspension de votre session " + session.getSessionNumber() + " a ete levee par " + actor + ".",
                session.getSessionNumber(), SESSIONS_PATH + session.getSessionNumber());
        return step(session, null, null, List.of());
    }

    // ------------------------------------------------------------------ cloture

    @Override
    public CashSessionStepResponse startClosing(String sessionNumber) {
        CashSession session = session(sessionNumber);
        requireStatus(session, CashSessionStatus.OPEN);
        requireCashier(session, actors.resolve(null), "demander la cloture");
        assertNoPendingMovement(session);
        session.setStatus(CashSessionStatus.COUNTING);
        session.setClosingRequestedAt(Instant.now());
        sessionRepository.save(session);
        return step(session, null, null, List.of(Boolean.TRUE.equals(session.getCashRegister().getBlindClose())
                ? "Comptez la caisse : l'attendu sera affiche apres validation du comptage"
                : "Comptez la caisse"));
    }

    @Override
    public CashSessionStepResponse submitClosingCount(String sessionNumber, CashCountRequest request) {
        CashSession session = session(sessionNumber);
        String actor = actors.resolve(null);
        CashSessionStatus status = session.getStatus();
        if (status == CashSessionStatus.OPEN) {
            // Forme historique : le comptage arrive sans demande de cloture prealable.
            requireCashier(session, actor, "clore la session");
            assertNoPendingMovement(session);
            session.setStatus(CashSessionStatus.COUNTING);
            session.setClosingRequestedAt(Instant.now());
            status = CashSessionStatus.COUNTING;
        }
        if (status == CashSessionStatus.COUNTING) {
            requireCashier(session, actor, "compter la cloture");
        } else if (status == CashSessionStatus.RECOUNT) {
            String firstCounter = countRepository.findBySessionNumberOrderByCreatedAtAsc(session.getSessionNumber()).stream()
                    .filter(count -> count.getMoment() == CashCountMoment.CLOSING || count.getMoment() == CashCountMoment.RECOUNT)
                    .reduce((first, second) -> second)
                    .map(CashCount::getCountedBy).orElse(null);
            if (CashActorResolver.samePerson(actor, firstCounter)) {
                throw new ForbiddenException("Le recomptage est fait par une autre personne que celle qui a compte");
            }
        } else {
            throw new BadRequestException("La session " + session.getSessionNumber() + " n'est pas en comptage");
        }
        return evaluateClosingCount(session, request, actor, status == CashSessionStatus.RECOUNT ? CashCountMoment.RECOUNT : CashCountMoment.CLOSING,
                false, null, null);
    }

    /**
     * Calcule l'ecart d'un comptage de cloture et en tire la suite : cloture, recomptage ou revue.
     */
    private CashSessionStepResponse evaluateClosingCount(CashSession session, CashCountRequest request, String actor,
                                                         CashCountMoment moment, boolean forced, String witness, String reference) {
        CashRegister register = session.getCashRegister();
        BigDecimal expected = money(summarySupport.expectedClosingAmount(session));
        CashCount count = countRecorder.record(register, session.getSessionNumber(), moment, request, actor, expected,
                reference, request == null ? null : trim(request.secondCounterEmail()), witness);
        countRecorder.supersede(session.getClosingCountNumber(), count.getCountNumber());
        BigDecimal counted = count.getTotal();
        BigDecimal variance = counted.subtract(expected);
        List<String> messages = new ArrayList<>();
        Instant now = Instant.now();

        // Un recomptage remplace l'ecart en revue precedent.
        reviewRepository.findBySessionNumberOrderByCreatedAtAsc(session.getSessionNumber()).stream()
                .filter(previous -> previous.getStatus() == CashVarianceStatus.PENDING
                        && (previous.getKind() == CashVarianceKind.CLOSING || previous.getKind() == CashVarianceKind.FORCED_CLOSE))
                .forEach(previous -> {
                    previous.setStatus(CashVarianceStatus.DECIDED);
                    previous.setDecisionNote("Remplace par le comptage " + count.getCountNumber());
                    previous.setDecidedAt(now);
                    previous.setDecidedBy(actor);
                    reviewRepository.save(previous);
                });

        session.setClosedBy(actor);
        session.setClosingAmount(counted);
        session.setCountedClosingAmount(counted);
        session.setExpectedClosingAmount(expected);
        session.setVarianceAmount(variance);
        session.setClosingCountNumber(count.getCountNumber());
        if (session.getClosingRequestedAt() == null) {
            session.setClosingRequestedAt(now);
        }
        BigDecimal kept = request != null && request.keptFloat() != null ? money(request.keptFloat())
                : register.getStandardFloat() == null ? counted : register.getStandardFloat().min(counted);
        if (kept.compareTo(counted) > 0) {
            throw new BadRequestException("Le fonds garde ne peut pas depasser le montant compte");
        }
        session.setKeptFloat(kept);
        session.setAmountToRemit(counted.subtract(kept));
        register.setLastCountAt(now);
        register.setLastCountAmount(counted);
        registerRepository.save(register);

        CashVarianceReview review = null;
        boolean doubleCountNeeded = !forced && moment == CashCountMoment.CLOSING && variance.signum() != 0
                && register.getDoubleCountThreshold() != null && register.getDoubleCountThreshold().signum() > 0
                && variance.abs().compareTo(register.getDoubleCountThreshold()) > 0;
        if (doubleCountNeeded) {
            session.setStatus(CashSessionStatus.RECOUNT);
            messages.add("L'ecart depasse le seuil de double comptage : une autre personne doit recompter la caisse");
            notifyReviewers("Recomptage necessaire · " + session.getSessionNumber(),
                    "La cloture de la session " + session.getSessionNumber() + " fait apparaitre un ecart de " + plain(variance)
                            + " XAF, au-dela du seuil de double comptage. Une autre personne que " + actor + " doit recompter.",
                    session.getSessionNumber());
        } else if (variance.signum() == 0 || (!forced && withinTolerance(register, expected, variance))) {
            if (variance.signum() != 0) {
                review = review(session, CashVarianceKind.CLOSING, variance, count.getCountNumber(),
                        "Ecart dans la tolerance de la caisse", CashVarianceStatus.TOLERATED);
                review.setDecision(CashVarianceDecision.TOLERATED);
                review.setDecidedAt(now);
                review.setDecidedBy("SYSTEM");
                reviewRepository.save(review);
                session.setVarianceDecision(CashVarianceDecision.TOLERATED);
                session.setVarianceReason("Ecart dans la tolerance");
                messages.add("Ecart de " + plain(variance) + " XAF dans la tolerance : session close, ecart comptabilise");
            }
            closeNow(session, now, forced ? "cloture d'office" : null);
        } else {
            String reason = forced ? (request == null || !StringUtils.hasText(request.note()) ? "Cloture d'office" : request.note().trim())
                    : varianceReason(request == null ? null : request.varianceReasonCode(),
                    request == null ? null : request.varianceReasonText(), "Indiquez le motif de l'ecart de cloture");
            session.setVarianceReason(reason);
            session.setStatus(CashSessionStatus.CLOSING_REVIEW);
            review = review(session, forced ? CashVarianceKind.FORCED_CLOSE : CashVarianceKind.CLOSING, variance,
                    count.getCountNumber(), reason, CashVarianceStatus.PENDING);
            if (forced) {
                review.setContestDeadline(now.plus(Duration.ofDays(contestDays)));
                reviewRepository.save(review);
            }
            notifyReviewers("Ecart de caisse a decider · " + session.getSessionNumber(),
                    "La session " + session.getSessionNumber() + " ouverte par " + session.getOpenedBy() + " a ete comptee par "
                            + actor + " : attendu " + plain(expected) + " XAF, compte " + plain(counted) + " XAF, ecart "
                            + plain(variance) + " XAF. Motif : " + reason + ".", session.getSessionNumber());
            messages.add("Ecart de " + plain(variance) + " XAF hors tolerance : session en revue");
        }
        // Remise de fin de session (lot C3) : ce qui depasse le fonds garde part a la caisse mere,
        // en transit jusqu'a sa reception comptee.
        if (session.getStatus() != CashSessionStatus.RECOUNT && session.getAmountToRemit() != null
                && session.getAmountToRemit().signum() > 0 && register.getParentRegisterCode() != null && operations != null) {
            var remittance = operations.remitAtClosing(session, session.getAmountToRemit(), actor);
            if (remittance != null) {
                register.setLastCountAmount(session.getKeptFloat());
                registerRepository.save(register);
                messages.add("Remise de " + plain(session.getAmountToRemit()) + " XAF a la caisse mere : transfert "
                        + remittance.transferNumber() + " en transit");
            }
        }
        sessionRepository.save(session);
        eventPublisher.publishEvent(new CashSessionClosedEvent(session.getSessionNumber()));
        return step(session, countRecorder.toResponse(count, true), toResponse(review), messages);
    }

    private void closeNow(CashSession session, Instant now, String cause) {
        session.setStatus(CashSessionStatus.CLOSED);
        session.setClosedAt(now);
        eventPublisher.publishEvent(new CashSessionFinalizedEvent(session.getSessionNumber(), false, cause, null));
    }

    static boolean withinTolerance(CashRegister register, BigDecimal expected, BigDecimal variance) {
        BigDecimal tolerance = register.getCountTolerance() == null ? BigDecimal.ZERO : register.getCountTolerance();
        if (register.getCountTolerancePercent() != null && register.getCountTolerancePercent().signum() > 0 && expected != null) {
            BigDecimal percent = expected.abs().multiply(register.getCountTolerancePercent())
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            tolerance = tolerance.max(percent);
        }
        return variance.abs().compareTo(tolerance) <= 0;
    }

    @Override
    public CashSessionStepResponse requestRecount(String sessionNumber, CashReasonRequest request) {
        CashSession session = session(sessionNumber);
        requireStatus(session, CashSessionStatus.CLOSING_REVIEW);
        String actor = actors.resolve(null);
        requireNotCashier(session, actor, "demander le recomptage de sa propre session");
        session.setStatus(CashSessionStatus.RECOUNT);
        sessionRepository.save(session);
        notifyReviewers("Recomptage demande · " + session.getSessionNumber(),
                actor + " demande le recomptage de la session " + session.getSessionNumber()
                        + (request != null && StringUtils.hasText(request.note()) ? " : " + request.note().trim() : "") + ".",
                session.getSessionNumber());
        return step(session, null, null, List.of("Recomptage demande : une autre personne que le premier compteur recompte"));
    }

    // ------------------------------------------------------------------ revues

    @Override
    public CashSessionStepResponse decideReview(String reviewNumber, CashVarianceDecisionRequest request) {
        CashVarianceReview review = reviewRepository.findByReviewNumber(reviewNumber == null ? null : reviewNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Ecart introuvable : " + reviewNumber));
        return decide(review, request);
    }

    @Override
    public CashSessionStepResponse decideSessionVariance(String sessionNumber, CashVarianceDecisionRequest request) {
        CashSession session = session(sessionNumber);
        if (session.getStatus() != CashSessionStatus.CLOSING_REVIEW) {
            throw new BadRequestException("La session " + session.getSessionNumber() + " n'attend pas de decision d'ecart");
        }
        CashVarianceReview review = reviewRepository.findBySessionNumberOrderByCreatedAtAsc(session.getSessionNumber()).stream()
                .filter(candidate -> candidate.getStatus() == CashVarianceStatus.PENDING
                        && EnumSet.of(CashVarianceKind.CLOSING, CashVarianceKind.FORCED_CLOSE, CashVarianceKind.HANDOVER).contains(candidate.getKind()))
                .reduce((first, second) -> second)
                .orElseGet(() -> review(session, CashVarianceKind.CLOSING,
                        session.getVarianceAmount() == null ? BigDecimal.ZERO : session.getVarianceAmount(),
                        session.getClosingCountNumber(), session.getVarianceReason(), CashVarianceStatus.PENDING));
        return decide(review, request);
    }

    private CashSessionStepResponse decide(CashVarianceReview review, CashVarianceDecisionRequest request) {
        if (review.getStatus() != CashVarianceStatus.PENDING) {
            throw new BadRequestException("Cet ecart est deja decide");
        }
        String actor = actors.resolve(null);
        CashSession session = review.getSessionNumber() == null ? null
                : sessionRepository.findBySessionNumber(review.getSessionNumber()).orElse(null);
        if (CashActorResolver.samePerson(actor, review.getCashier())
                || (session != null && (CashActorResolver.samePerson(actor, session.getOpenedBy())
                || (session.getClosedBy() != null && !Boolean.TRUE.equals(session.getForcedClose())
                && CashActorResolver.samePerson(actor, session.getClosedBy()))))) {
            throw new ForbiddenException("Un caissier ne peut pas decider de l'ecart de sa propre session : la decision revient a une autre personne");
        }
        Instant now = Instant.now();
        if (review.getContestDeadline() != null && review.getContestedAt() == null && now.isBefore(review.getContestDeadline())) {
            throw new BadRequestException("Le caissier peut contester cette cloture d'office jusqu'au " + review.getContestDeadline()
                    + " : l'ecart ne sera impute qu'apres ce delai ou sa contestation");
        }
        CashVarianceDecision decision = request == null || request.decision() == null ? proposedDecision(review.getAmount()) : request.decision();
        if (decision == CashVarianceDecision.TOLERATED) {
            throw new BadRequestException("Choisissez une imputation : charge, produit, caissier ou mise en recherche");
        }
        review.setStatus(CashVarianceStatus.DECIDED);
        review.setDecision(decision);
        review.setDecidedBy(actor);
        review.setDecidedAt(now);
        review.setDecisionNote(request == null ? null : trim(request.note()));
        if (decision == CashVarianceDecision.INVESTIGATION) {
            review.setReviewDueAt(request != null && request.reviewDueAt() != null ? request.reviewDueAt()
                    : now.plus(Duration.ofDays(investigationDays)));
        }
        reviewRepository.save(review);
        List<String> messages = new ArrayList<>();
        if (session != null && session.getStatus() == CashSessionStatus.CLOSING_REVIEW
                && review.getKind() != CashVarianceKind.OPENING && review.getKind() != CashVarianceKind.SPOT_CHECK) {
            session.setReviewedBy(actor);
            session.setReviewedAt(now);
            session.setVarianceDecision(decision);
            session.setVarianceDecidedBy(actor);
            if (StringUtils.hasText(review.getDecisionNote())) {
                String current = trim(session.getVarianceReason());
                session.setVarianceReason((current == null ? "" : current + " | ") + "Revue : " + review.getDecisionNote());
            }
            closeNow(session, now, "Ecart decide par " + actor);
            sessionRepository.save(session);
            messages.add("Session close");
        }
        if (session != null) {
            emailNotifier.notifyUser(session.getOpenedBy(), "Ecart de caisse decide · " + session.getSessionNumber(),
                    "L'ecart de " + plain(review.getAmount()) + " XAF constate sur votre session " + session.getSessionNumber()
                            + " a ete decide par " + actor + " : " + decisionLabel(decision)
                            + (StringUtils.hasText(review.getDecisionNote()) ? " · Note : " + review.getDecisionNote() : "") + ".",
                    session.getSessionNumber(), SESSIONS_PATH + session.getSessionNumber());
        }
        return step(session, null, toResponse(review), messages);
    }

    static CashVarianceDecision proposedDecision(BigDecimal amount) {
        return amount != null && amount.signum() > 0 ? CashVarianceDecision.INCOME : CashVarianceDecision.EXPENSE;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CashVarianceReviewResponse> reviews(CashVarianceStatus status, String registerCode, String sessionNumber) {
        List<CashVarianceReview> reviews;
        if (StringUtils.hasText(sessionNumber)) {
            reviews = reviewRepository.findBySessionNumberOrderByCreatedAtAsc(sessionNumber.trim());
        } else if (StringUtils.hasText(registerCode)) {
            reviews = reviewRepository.findByRegisterCodeOrderByCreatedAtDesc(registerCode.trim());
        } else {
            reviews = reviewRepository.findByStatusOrderByCreatedAtAsc(status == null ? CashVarianceStatus.PENDING : status);
        }
        return reviews.stream().filter(review -> status == null || review.getStatus() == status).map(this::toResponse).toList();
    }

    // ------------------------------------------------- comptage inopine, d'office

    @Override
    public CashSessionStepResponse spotCheck(String sessionNumber, CashCountRequest request) {
        CashSession session = session(sessionNumber);
        if (!EnumSet.of(CashSessionStatus.OPEN, CashSessionStatus.PAUSED, CashSessionStatus.SUSPENDED).contains(session.getStatus())) {
            throw new BadRequestException("Un comptage inopine se fait sur une session en cours");
        }
        String actor = actors.resolve(null);
        requireNotCashier(session, actor, "faire un comptage inopine de sa propre caisse");
        BigDecimal expected = money(summarySupport.expectedClosingAmount(session));
        CashCount count = countRecorder.record(session.getCashRegister(), session.getSessionNumber(), CashCountMoment.SPOT_CHECK,
                request, actor, expected, sequenceGenerator.next("cash_spot_check"), null, null);
        BigDecimal variance = count.getTotal().subtract(expected);
        CashVarianceReview review = null;
        List<String> messages = new ArrayList<>();
        if (variance.signum() != 0) {
            boolean tolerated = withinTolerance(session.getCashRegister(), expected, variance);
            review = review(session, CashVarianceKind.SPOT_CHECK, variance, count.getCountNumber(),
                    request == null ? null : trim(request.note()), tolerated ? CashVarianceStatus.TOLERATED : CashVarianceStatus.PENDING);
            messages.add("Ecart de " + plain(variance) + " XAF constate" + (tolerated ? " (dans la tolerance)" : " : en revue"));
        } else {
            messages.add("Aucun ecart");
        }
        CashRegister register = session.getCashRegister();
        register.setLastCountAt(Instant.now());
        register.setLastCountAmount(count.getTotal());
        registerRepository.save(register);
        return step(session, countRecorder.toResponse(count, true), toResponse(review), messages);
    }

    @Override
    public CashSessionStepResponse forceClose(String sessionNumber, CashCountRequest request, CashReasonRequest reason) {
        CashSession session = session(sessionNumber);
        if (!EnumSet.of(CashSessionStatus.OPENING, CashSessionStatus.OPEN, CashSessionStatus.PAUSED, CashSessionStatus.SUSPENDED,
                CashSessionStatus.COUNTING).contains(session.getStatus())) {
            throw new BadRequestException("La session " + session.getSessionNumber() + " ne peut pas etre close d'office dans son etat actuel");
        }
        String actor = actors.resolve(null);
        requireNotCashier(session, actor, "clore d'office sa propre session");
        String witness = request == null || !StringUtils.hasText(request.witnessEmail()) ? null : request.witnessEmail().trim().toLowerCase(Locale.ROOT);
        if (witness == null) {
            throw new BadRequestException("Une cloture d'office se compte avec un temoin : indiquez-le");
        }
        if (CashActorResolver.samePerson(witness, actor) || CashActorResolver.samePerson(witness, session.getOpenedBy())) {
            throw new BadRequestException("Le temoin doit etre une autre personne que le superviseur et le caissier");
        }
        session.setForcedClose(true);
        session.setStatus(CashSessionStatus.COUNTING);
        String note = reason == null ? null : trim(reason.note());
        CashCountRequest withNote = request == null ? null : new CashCountRequest(request.currency(), request.lines(), request.totalAmount(),
                request.secondCounterEmail(), witness, request.keptFloat(), null, null,
                note == null ? "Cloture d'office" : "Cloture d'office : " + note);
        CashSessionStepResponse result = evaluateClosingCount(session, withNote, actor, CashCountMoment.FORCED_CLOSE, true, witness,
                sequenceGenerator.next("cash_forced_close"));
        emailNotifier.notifyUser(session.getOpenedBy(), "Cloture d'office de votre session · " + session.getSessionNumber(),
                "Votre session " + session.getSessionNumber() + " a ete close d'office par " + actor + " avec " + witness
                        + " pour temoin. Montant compte : " + plain(session.getCountedClosingAmount()) + " XAF."
                        + (session.getStatus() == CashSessionStatus.CLOSING_REVIEW
                        ? " Vous pouvez contester ce comptage pendant " + contestDays + " jours." : ""),
                session.getSessionNumber(), SESSIONS_PATH + session.getSessionNumber());
        return result;
    }

    @Override
    public CashSessionStepResponse contest(String sessionNumber, CashReasonRequest request) {
        CashSession session = session(sessionNumber);
        String actor = actors.resolve(null);
        requireCashier(session, actor, "contester la cloture d'office");
        CashVarianceReview review = reviewRepository.findBySessionNumberOrderByCreatedAtAsc(session.getSessionNumber()).stream()
                .filter(candidate -> candidate.getKind() == CashVarianceKind.FORCED_CLOSE && candidate.getStatus() == CashVarianceStatus.PENDING)
                .reduce((first, second) -> second)
                .orElseThrow(() -> new BadRequestException("Aucune cloture d'office contestable sur cette session"));
        if (review.getContestedAt() != null) {
            throw new BadRequestException("Cette cloture d'office est deja contestee");
        }
        if (review.getContestDeadline() != null && Instant.now().isAfter(review.getContestDeadline())) {
            throw new BadRequestException("Le delai de contestation est depasse");
        }
        String text = request == null ? null : (StringUtils.hasText(request.reasonText()) ? request.reasonText().trim() : trim(request.note()));
        if (text == null) {
            throw new BadRequestException("Expliquez la contestation");
        }
        review.setContestedAt(Instant.now());
        review.setContestReason(text);
        reviewRepository.save(review);
        notifyReviewers("Cloture d'office contestee · " + session.getSessionNumber(),
                session.getOpenedBy() + " conteste le comptage de la cloture d'office de la session " + session.getSessionNumber() + " : " + text,
                session.getSessionNumber());
        return step(session, null, toResponse(review), List.of("Contestation enregistree : l'ecart sera revu"));
    }

    @Override
    public CashSessionStepResponse cancel(String sessionNumber, CashReasonRequest request) {
        CashSession session = session(sessionNumber);
        if (session.getStatus() != CashSessionStatus.OPENING && session.getStatus() != CashSessionStatus.OPEN) {
            throw new BadRequestException("Seule une session ouverte sans operation peut etre annulee");
        }
        String actor = actors.resolve(null);
        if (!CashActorResolver.samePerson(actor, session.getOpenedBy()) && !actors.hasAuthority(DECIDE)) {
            throw new ForbiddenException("Seul le caissier de la session ou un superviseur peut l'annuler");
        }
        List<CashMovement> movements = movementRepository.findByCashSession_IdOrderByCreatedAtAsc(session.getId());
        boolean hasOperations = movements.stream().anyMatch(movement -> movement.getMovementType() != CashMovementType.OPENING_FLOAT);
        if (hasOperations) {
            throw new BadRequestException("La session " + session.getSessionNumber() + " a deja des operations : elle se clot, elle ne s'annule pas");
        }
        String reason = referenceLists.describe(SESSION_CANCEL_REASON, request == null ? null : request.reasonCode(),
                request == null ? null : request.reasonText());
        movements.forEach(movement -> {
            movement.setStatus(CashMovementStatus.CANCELLED);
            movementRepository.save(movement);
        });
        session.setStatus(CashSessionStatus.CANCELLED);
        session.setCancelReason(reason);
        session.setClosedBy(actor);
        session.setClosedAt(Instant.now());
        sessionRepository.save(session);
        return step(session, null, null, List.of("Session annulee, sans trace financiere"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CashCountResponse> counts(String sessionNumber) {
        CashSession session = session(sessionNumber);
        boolean reveal = revealExpected(session);
        return countRepository.findBySessionNumberOrderByCreatedAtAsc(session.getSessionNumber()).stream()
                .map(count -> countRecorder.toResponse(count, reveal || count.getMoment() != CashCountMoment.CLOSING
                        || session.getStatus() != CashSessionStatus.RECOUNT))
                .toList();
    }

    // ------------------------------------------------------------------ passation

    @Override
    public CashHandoverResponse startHandover(String sessionNumber, CashHandoverRequest request) {
        CashSession session = session(sessionNumber);
        requireStatus(session, CashSessionStatus.OPEN);
        String actor = actors.resolve(null);
        requireCashier(session, actor, "passer la caisse");
        assertNoPendingMovement(session);
        String incoming = request.incomingEmail().trim().toLowerCase(Locale.ROOT);
        if (CashActorResolver.samePerson(incoming, actor)) {
            throw new BadRequestException("L'entrant doit etre une autre personne");
        }
        var assignment = accessGuard.effectiveAssignment(session.getCashRegister().getRegisterCode(), incoming, Instant.now());
        if (assignment.isEmpty() || assignment.get().getRole() == CashAssignmentRole.VIEWER) {
            throw new BadRequestException(incoming + " n'est pas affecte a cette caisse : affectez-le d'abord (ou en urgence)");
        }
        BigDecimal expected = money(summarySupport.expectedClosingAmount(session));
        CashCount count = countRecorder.record(session.getCashRegister(), session.getSessionNumber(), CashCountMoment.HANDOVER,
                request.count(), actor, expected, null, null, null);
        String number = sequenceGenerator.next("cash_handover");
        count.setReferenceNumber(number);
        countRepository.save(count);
        session.setStatus(CashSessionStatus.COUNTING);
        session.setClosingRequestedAt(Instant.now());
        session.setHandoverNumber(number);
        session.setVarianceReason(request.count() == null ? null
                : optionalVarianceReason(request.count().varianceReasonCode(), request.count().varianceReasonText()));
        sessionRepository.save(session);
        CashHandover handover = handoverRepository.save(CashHandover.builder()
                .handoverNumber(number)
                .registerCode(session.getCashRegister().getRegisterCode())
                .fromSession(session.getSessionNumber())
                .outgoing(actor)
                .incoming(incoming)
                .outgoingCount(count.getCountNumber())
                .outgoingAmount(count.getTotal())
                .status(CashHandoverStatus.PENDING)
                .outgoingSignedAt(Instant.now())
                .build());
        emailNotifier.notifyUser(incoming, "Passation de caisse a confirmer · " + number,
                actor + " vous passe la caisse " + session.getCashRegister().getName() + " avec un fonds compte de "
                        + plain(count.getTotal()) + " XAF. Comptez-le et confirmez, ou contestez.",
                number, SESSIONS_PATH + session.getSessionNumber());
        return toResponse(handover);
    }

    @Override
    public CashSessionStepResponse acceptHandover(String handoverNumber, CashHandoverAnswerRequest request) {
        CashHandover handover = pendingHandover(handoverNumber);
        String actor = actors.resolve(null);
        if (!CashActorResolver.samePerson(actor, handover.getIncoming())) {
            throw new ForbiddenException("Seul l'entrant confirme la passation");
        }
        CashSession outgoing = session(handover.getFromSession());
        if (request != null && request.count() != null) {
            BigDecimal incomingTotal = countRecorder.totals(outgoing.getCashRegister(), request.count()).total();
            if (incomingTotal.compareTo(handover.getOutgoingAmount()) != 0) {
                return disputeHandover(handoverNumber, request);
            }
            CashCount count = countRecorder.record(outgoing.getCashRegister(), outgoing.getSessionNumber(), CashCountMoment.HANDOVER,
                    request.count(), actor, handover.getOutgoingAmount(), handover.getHandoverNumber(), null, null);
            handover.setIncomingCount(count.getCountNumber());
        }
        handover.setIncomingAmount(handover.getOutgoingAmount());
        handover.setIncomingSignedAt(Instant.now());
        handover.setStatus(CashHandoverStatus.ACCEPTED);
        return finishHandover(handover, outgoing, handover.getOutgoingAmount(), request == null ? null : request.deviceCode(), null);
    }

    @Override
    public CashSessionStepResponse disputeHandover(String handoverNumber, CashHandoverAnswerRequest request) {
        CashHandover handover = pendingHandover(handoverNumber);
        String actor = actors.resolve(null);
        if (!CashActorResolver.samePerson(actor, handover.getIncoming())) {
            throw new ForbiddenException("Seul l'entrant conteste la passation");
        }
        if (request == null || request.count() == null) {
            throw new BadRequestException("Pour contester, saisissez votre propre comptage");
        }
        if (!StringUtils.hasText(request.reason())) {
            throw new BadRequestException("Expliquez le desaccord");
        }
        CashSession outgoing = session(handover.getFromSession());
        CashCount count = countRecorder.record(outgoing.getCashRegister(), outgoing.getSessionNumber(), CashCountMoment.HANDOVER,
                request.count(), actor, handover.getOutgoingAmount(), handover.getHandoverNumber(), null, null);
        handover.setIncomingCount(count.getCountNumber());
        handover.setIncomingAmount(count.getTotal());
        handover.setIncomingSignedAt(Instant.now());
        handover.setStatus(CashHandoverStatus.DISPUTED);
        handover.setDisputeReason(request.reason().trim());
        return finishHandover(handover, outgoing, count.getTotal(), request.deviceCode(), request.reason().trim());
    }

    /**
     * Clot la session du sortant sur le montant retenu et ouvre celle de l'entrant avec ce montant.
     * Un desaccord met la session du sortant en revue, comme un ecart.
     */
    private CashSessionStepResponse finishHandover(CashHandover handover, CashSession outgoing, BigDecimal retained,
                                                   String deviceCode, String disputeReason) {
        Instant now = Instant.now();
        BigDecimal expected = money(summarySupport.expectedClosingAmount(outgoing));
        BigDecimal variance = retained.subtract(expected);
        CashRegister register = outgoing.getCashRegister();
        outgoing.setClosedBy(handover.getOutgoing());
        outgoing.setClosingAmount(retained);
        outgoing.setCountedClosingAmount(retained);
        outgoing.setExpectedClosingAmount(expected);
        outgoing.setVarianceAmount(variance);
        outgoing.setKeptFloat(retained);
        outgoing.setAmountToRemit(BigDecimal.ZERO.setScale(4));
        outgoing.setClosingCountNumber(handover.getIncomingCount() != null ? handover.getIncomingCount() : handover.getOutgoingCount());
        CashVarianceReview review = null;
        List<String> messages = new ArrayList<>();
        if (disputeReason != null || (variance.signum() != 0 && !withinTolerance(register, expected, variance))) {
            String reason = disputeReason != null ? "Passation contestee par " + handover.getIncoming() + " : " + disputeReason
                    : (outgoing.getVarianceReason() == null ? "Ecart constate a la passation" : outgoing.getVarianceReason());
            outgoing.setVarianceReason(reason);
            outgoing.setStatus(CashSessionStatus.CLOSING_REVIEW);
            review = review(outgoing, disputeReason != null ? CashVarianceKind.HANDOVER : CashVarianceKind.CLOSING, variance,
                    outgoing.getClosingCountNumber(), reason, CashVarianceStatus.PENDING);
            notifyReviewers("Passation a revoir · " + handover.getHandoverNumber(), reason + ". Attendu " + plain(expected)
                    + " XAF, retenu " + plain(retained) + " XAF.", outgoing.getSessionNumber());
            messages.add("Ecart ou desaccord a la passation : en revue");
        } else {
            if (variance.signum() != 0) {
                review = review(outgoing, CashVarianceKind.CLOSING, variance, outgoing.getClosingCountNumber(),
                        "Ecart dans la tolerance a la passation", CashVarianceStatus.TOLERATED);
                review.setDecision(CashVarianceDecision.TOLERATED);
                reviewRepository.save(review);
                outgoing.setVarianceDecision(CashVarianceDecision.TOLERATED);
            }
            closeNow(outgoing, now, "passation " + handover.getHandoverNumber());
        }
        register.setLastCountAt(now);
        register.setLastCountAmount(retained);
        registerRepository.save(register);
        sessionRepository.save(outgoing);
        CashSessionResponse incomingSession = cashRegisterService.openSessionFromHandover(register.getRegisterCode(),
                handover.getIncoming(), retained, handover.getHandoverNumber(), deviceCode);
        handover.setToSession(incomingSession.sessionNumber());
        handoverRepository.save(handover);
        messages.add("Session " + incomingSession.sessionNumber() + " ouverte pour " + handover.getIncoming() + " avec "
                + plain(retained) + " XAF");
        eventPublisher.publishEvent(new CashSessionClosedEvent(outgoing.getSessionNumber()));
        return new CashSessionStepResponse(incomingSession, null, toResponse(review), messages);
    }

    @Override
    public CashHandoverResponse cancelHandover(String handoverNumber) {
        CashHandover handover = pendingHandover(handoverNumber);
        String actor = actors.resolve(null);
        if (!CashActorResolver.samePerson(actor, handover.getOutgoing())) {
            throw new ForbiddenException("Seul le sortant annule la passation");
        }
        CashSession outgoing = session(handover.getFromSession());
        outgoing.setStatus(CashSessionStatus.OPEN);
        outgoing.setHandoverNumber(null);
        outgoing.setClosingRequestedAt(null);
        sessionRepository.save(outgoing);
        handover.setStatus(CashHandoverStatus.CANCELLED);
        return toResponse(handoverRepository.save(handover));
    }

    private CashHandover pendingHandover(String handoverNumber) {
        CashHandover handover = handoverRepository.findByHandoverNumber(handoverNumber == null ? null : handoverNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Passation introuvable : " + handoverNumber));
        if (handover.getStatus() != CashHandoverStatus.PENDING) {
            throw new BadRequestException("Cette passation n'est plus en attente");
        }
        return handover;
    }

    // ------------------------------------------------------------------ coupures

    @Override
    @Transactional(readOnly = true)
    public List<CashDenominationResponse> denominations(String currency, boolean includeRetired) {
        String resolved = StringUtils.hasText(currency) ? currency.trim().toUpperCase(Locale.ROOT) : CashCountRecorder.DEFAULT_CURRENCY;
        LocalDate today = LocalDate.now();
        return denominationRepository.findByCurrencyOrderBySortOrderAsc(resolved).stream()
                .filter(denomination -> includeRetired || CashCountRecorder.isActive(denomination, today))
                .map(denomination -> new CashDenominationResponse(denomination.getCurrency(), denomination.getValue(), denomination.getKind(),
                        denomination.getLabel(), denomination.getSortOrder(), denomination.getActiveFrom(), denomination.getActiveTo(),
                        CashCountRecorder.isActive(denomination, today)))
                .toList();
    }

    @Override
    public List<CashDenominationResponse> replaceDenominations(String currency, List<CashDenominationRequest> values) {
        String resolved = StringUtils.hasText(currency) ? currency.trim().toUpperCase(Locale.ROOT) : CashCountRecorder.DEFAULT_CURRENCY;
        Map<String, CashDenomination> existing = denominationRepository.findByCurrencyOrderBySortOrderAsc(resolved).stream()
                .collect(Collectors.toMap(denomination -> key(denomination.getValue(), denomination.getKind().name()), Function.identity()));
        LocalDate today = LocalDate.now();
        int order = 10;
        List<CashDenomination> saved = new ArrayList<>();
        for (CashDenominationRequest request : values == null ? List.<CashDenominationRequest>of() : values) {
            CashDenomination denomination = existing.remove(key(request.value(), request.kind().name()));
            if (denomination == null) {
                denomination = CashDenomination.builder().currency(resolved).value(money(request.value())).kind(request.kind()).build();
            }
            denomination.setLabel(request.label().trim());
            denomination.setSortOrder(request.sortOrder() == null ? order : request.sortOrder());
            denomination.setActiveFrom(request.activeFrom() == null ? (denomination.getActiveFrom() == null ? today : denomination.getActiveFrom()) : request.activeFrom());
            denomination.setActiveTo(request.activeTo());
            saved.add(denomination);
            order += 10;
        }
        // Une coupure absente est retiree de la circulation, pas supprimee : les comptages passes la gardent.
        for (CashDenomination retired : existing.values()) {
            if (retired.getActiveTo() == null || retired.getActiveTo().isAfter(today)) {
                retired.setActiveTo(today.minusDays(1));
                saved.add(retired);
            }
        }
        denominationRepository.saveAll(saved);
        return denominations(resolved, true);
    }

    private static String key(BigDecimal value, String kind) {
        return value.stripTrailingZeros().toPlainString() + "/" + kind;
    }

    // ------------------------------------------------------------------ outils

    private CashSession session(String sessionNumber) {
        if (!StringUtils.hasText(sessionNumber)) {
            throw new BadRequestException("Numero de session manquant");
        }
        return sessionRepository.findBySessionNumber(sessionNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Session de caisse introuvable : " + sessionNumber));
    }

    private void assertNoPendingMovement(CashSession session) {
        if (session.getId() != null && !movementRepository.findByCashSession_IdAndStatus(session.getId(), CashMovementStatus.PENDING).isEmpty()) {
            throw new BadRequestException("Des operations attendent un visa sur la session " + session.getSessionNumber()
                    + " : faites-les viser ou annulez-les avant de clore");
        }
    }

    private static void requireStatus(CashSession session, CashSessionStatus status) {
        if (session.getStatus() != status) {
            throw new BadRequestException("La session " + session.getSessionNumber() + " est " + statusLabel(session.getStatus())
                    + " : cette action demande une session " + statusLabel(status));
        }
    }

    private static void requireCashier(CashSession session, String actor, String action) {
        if (!CashActorResolver.samePerson(actor, session.getOpenedBy())) {
            throw new ForbiddenException("Seul le caissier de la session peut " + action);
        }
    }

    private static void requireNotCashier(CashSession session, String actor, String action) {
        if (CashActorResolver.samePerson(actor, session.getOpenedBy())) {
            throw new ForbiddenException("Un caissier ne peut pas " + action);
        }
    }

    private void checkPassword(String actor, String password) {
        if (!StringUtils.hasText(password)) {
            throw new BadRequestException("Saisissez votre mot de passe pour reprendre la session");
        }
        var user = userRepository.findByEmailIgnoreCase(actor).orElse(null);
        if (user == null || user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ForbiddenException("Mot de passe incorrect");
        }
    }

    private String varianceReason(String code, String text, String missingMessage) {
        if (StringUtils.hasText(code)) {
            return referenceLists.describe(VARIANCE_REASON, code, text);
        }
        if (StringUtils.hasText(text)) {
            // Forme historique : un motif libre, sans code de la liste.
            return text.trim();
        }
        throw new BadRequestException(missingMessage);
    }

    private String optionalVarianceReason(String code, String text) {
        if (!StringUtils.hasText(code) && !StringUtils.hasText(text)) {
            return null;
        }
        return varianceReason(code, text, null);
    }

    private CashVarianceReview review(CashSession session, CashVarianceKind kind, BigDecimal amount, String countNumber,
                                      String reason, CashVarianceStatus status) {
        return reviewRepository.save(CashVarianceReview.builder()
                .reviewNumber(sequenceGenerator.next("cash_variance_review"))
                .registerCode(session.getCashRegister().getRegisterCode())
                .sessionNumber(session.getSessionNumber())
                .kind(kind)
                .amount(money(amount))
                .countNumber(countNumber)
                .cashier(session.getOpenedBy())
                .reason(reason)
                .status(status)
                .build());
    }

    /** Previent les reviseurs, et leurs delegues en cas d'absence. */
    private void notifyReviewers(String subject, String message, String sessionNumber) {
        emailNotifier.notifySupervisor(subject, message, sessionNumber, SESSIONS_PATH + sessionNumber);
        LocalDate today = LocalDate.now();
        Set<String> delegates = delegationRepository.findByCancelledAtIsNullOrderByStartsOnAsc().stream()
                .filter(delegation -> !delegation.getStartsOn().isAfter(today) && !delegation.getEndsOn().isBefore(today))
                .filter(delegation -> delegation.getPermissions().contains("CASH_VARIANCE_DECIDE"))
                .map(CashDelegation::getDelegate)
                .collect(Collectors.toSet());
        delegates.forEach(delegate -> emailNotifier.notifyUser(delegate, subject, message + " (vous remplacez un reviseur absent)",
                sessionNumber, SESSIONS_PATH + sessionNumber));
    }

    private boolean revealExpected(CashSession session) {
        return !Boolean.TRUE.equals(session.getCashRegister().getBlindClose()) || actors.hasAuthority(DECIDE);
    }

    private CashSessionStepResponse step(CashSession session, com.sni.bokaticowork.features.payment.dto.response.CashCountResponse count,
                                         com.sni.bokaticowork.features.payment.dto.response.CashVarianceReviewResponse review, List<String> messages) {
        return new CashSessionStepResponse(session == null ? null : mapper.toCashSessionResponse(session), count, review,
                messages == null ? List.of() : messages);
    }

    CashVarianceReviewResponse toResponse(CashVarianceReview review) {
        if (review == null) {
            return null;
        }
        Instant now = Instant.now();
        boolean decidable = review.getStatus() == CashVarianceStatus.PENDING
                && (review.getContestDeadline() == null || review.getContestedAt() != null || now.isAfter(review.getContestDeadline()));
        return new CashVarianceReviewResponse(review.getReviewNumber(), review.getRegisterCode(), review.getSessionNumber(), review.getKind(),
                review.getAmount(), review.getCountNumber(), review.getCashier(), review.getReason(), review.getStatus(), review.getDecision(),
                proposedDecision(review.getAmount()), review.getDecidedBy(), review.getDecidedAt(), review.getDecisionNote(),
                review.getContestDeadline(), review.getContestedAt(), review.getContestReason(), review.getReviewDueAt(), decidable,
                review.getCreatedAt());
    }

    private CashHandoverResponse toResponse(CashHandover handover) {
        return new CashHandoverResponse(handover.getHandoverNumber(), handover.getRegisterCode(), handover.getFromSession(),
                handover.getToSession(), handover.getOutgoing(), handover.getIncoming(), handover.getOutgoingCount(),
                handover.getIncomingCount(), handover.getOutgoingAmount(), handover.getIncomingAmount(), handover.getStatus(),
                handover.getDisputeReason(), handover.getOutgoingSignedAt(), handover.getIncomingSignedAt(), handover.getCreatedAt());
    }

    static String decisionLabel(CashVarianceDecision decision) {
        return switch (decision) {
            case EXPENSE -> "passe en charge";
            case INCOME -> "passe en produit";
            case CASHIER -> "impute au caissier";
            case INVESTIGATION -> "mis en recherche";
            case TOLERATED -> "dans la tolerance";
        };
    }

    static String statusLabel(CashSessionStatus status) {
        return switch (status) {
            case OPENING -> "en cours d'ouverture";
            case OPEN -> "ouverte";
            case PAUSED -> "en pause";
            case SUSPENDED -> "suspendue";
            case COUNTING -> "en comptage";
            case CLOSING_REVIEW -> "en revue d'ecart";
            case RECOUNT -> "en recomptage";
            case CLOSED -> "close";
            case CANCELLED -> "annulee";
        };
    }

    private static BigDecimal money(BigDecimal amount) {
        return (amount == null ? BigDecimal.ZERO : amount).setScale(4, RoundingMode.HALF_UP);
    }

    private static String plain(BigDecimal amount) {
        return amount == null ? "0" : amount.stripTrailingZeros().toPlainString();
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
