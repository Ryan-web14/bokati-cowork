package com.sni.bokaticowork.features.payment.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.payment.dto.request.ReviewCashAnomalyRequest;
import com.sni.bokaticowork.features.payment.dto.response.CashAnomalyFlagResponse;
import com.sni.bokaticowork.features.payment.enums.CashAnomalySeverity;
import com.sni.bokaticowork.features.payment.enums.CashAnomalyStatus;
import com.sni.bokaticowork.features.payment.enums.CashAnomalyType;
import com.sni.bokaticowork.features.payment.model.CashAnomalyFlag;
import com.sni.bokaticowork.features.payment.model.CashMovement;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.repository.CashAnomalyFlagRepository;
import com.sni.bokaticowork.features.payment.repository.CashMovementRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterRepository;
import com.sni.bokaticowork.features.payment.repository.CashSessionRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.CashAnomalyDetectionService;
import com.sni.bokaticowork.features.payment.service.support.CashEmailNotifier;
import com.sni.bokaticowork.features.support.dto.SupportDtos.CreateTicketRequest;
import com.sni.bokaticowork.features.support.enums.TicketCategory;
import com.sni.bokaticowork.features.support.enums.TicketPriority;
import com.sni.bokaticowork.features.support.service.interfaces.SupportTicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CashAnomalyDetectionServiceImpl implements CashAnomalyDetectionService {

    private static final String CASH_ANOMALIES_PATH = "/payments/cash-registers/anomalies/";
    private static final BigDecimal ROUND_NUMBER_DIVISOR = new BigDecimal("5000");
    private static final BigDecimal ROUND_PATTERN_RATIO_THRESHOLD = new BigDecimal("0.5");
    private static final BigDecimal EXCESSIVE_REFUND_RATIO_THRESHOLD = new BigDecimal("0.3");
    private static final BigDecimal STRUCTURING_LOWER_RATIO = new BigDecimal("0.8");
    private static final BigDecimal NEAR_MAX_RATIO = new BigDecimal("0.9");
    private static final int OFF_HOURS_START = 22;
    private static final int OFF_HOURS_END = 6;

    private final CashAnomalyFlagRepository anomalyFlagRepository;
    private final CashSessionRepository sessionRepository;
    private final CashMovementRepository movementRepository;
    private final CashRegisterRepository cashRegisterRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final CashEmailNotifier emailNotifier;
    @Lazy private final SupportTicketService supportTicketService;

    @Value("${bokati.payment.cash-register.alert-cooldown-hours:6}")
    private int alertCooldownHours;

    @Override
    @Transactional
    public List<CashAnomalyFlagResponse> analyzeSession(String sessionNumber) {
        CashSession session = session(sessionNumber);
        List<CashMovement> movements = movementRepository.findByCashSession_IdOrderByCreatedAtAsc(session.getId());

        List<CashAnomalyFlag> newFlags = new ArrayList<>();
        detectVarianceOutlier(session, newFlags);
        detectRoundNumberPattern(session, movements, newFlags);
        detectExcessiveRefunds(session, movements, newFlags);
        detectExcessiveAdjustments(session, movements, newFlags);
        detectOffHoursSession(session, newFlags);
        detectThresholdStructuring(session, movements, newFlags);
        detectNearMaxCashRecurrence(session, movements, newFlags);
        detectMaxCashOverflow(session, movements);

        return newFlags.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<CashAnomalyFlagResponse> list(String registerCode, String sessionNumber, CashAnomalySeverity severity,
                                                            CashAnomalyStatus status, CashAnomalyType anomalyType, Pageable pageable) {
        Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return new PaginatedResponse<>(anomalyFlagRepository.search(
                blankToNull(registerCode),
                blankToNull(sessionNumber),
                severity == null ? null : severity.name(),
                status == null ? null : status.name(),
                anomalyType == null ? null : anomalyType.name(),
                unsortedPageable
        ).map(this::toResponse));
    }

    @Override
    @Transactional
    public CashAnomalyFlagResponse review(String flagNumber, ReviewCashAnomalyRequest request) {
        CashAnomalyFlag flag = find(flagNumber);
        if (flag.getStatus() != CashAnomalyStatus.OPEN && flag.getStatus() != CashAnomalyStatus.ACKNOWLEDGED) {
            throw new BadRequestException("Anomaly flag " + flagNumber + " has already been resolved");
        }
        flag.setStatus(request.status());
        flag.setReviewedBy(request.reviewedBy().trim());
        flag.setReviewedAt(Instant.now());
        flag.setReviewNote(StringUtils.hasText(request.note()) ? request.note().trim() : null);
        flag = anomalyFlagRepository.save(flag);
        return toResponse(flag);
    }

    // ---- Detection rules ----

    private void detectVarianceOutlier(CashSession session, List<CashAnomalyFlag> newFlags) {
        BigDecimal variance = session.getVarianceAmount();
        if (variance == null || variance.signum() == 0 || !StringUtils.hasText(session.getOpenedBy())) {
            return;
        }
        Object[] stats = anomalyFlagRepository.cashierVarianceStats(session.getOpenedBy());
        BigDecimal average = toBigDecimal(stats != null && stats.length > 0 ? stats[0] : null);
        BigDecimal stddev = toBigDecimal(stats != null && stats.length > 1 ? stats[1] : null);

        BigDecimal deviation = variance.subtract(average).abs();
        CashAnomalySeverity severity;
        BigDecimal score;
        String description;
        if (stddev.signum() > 0) {
            BigDecimal zScore = deviation.divide(stddev, 4, RoundingMode.HALF_UP);
            if (zScore.compareTo(new BigDecimal("3")) > 0) {
                severity = CashAnomalySeverity.HIGH;
            } else if (zScore.compareTo(new BigDecimal("2")) > 0) {
                severity = CashAnomalySeverity.MEDIUM;
            } else {
                return;
            }
            score = zScore.setScale(2, RoundingMode.HALF_UP);
            description = "Écart de caisse de " + variance + " s'écartant fortement de la moyenne habituelle ("
                    + average.setScale(2, RoundingMode.HALF_UP) + ", écart-type " + stddev.setScale(2, RoundingMode.HALF_UP)
                    + ") du caissier " + session.getOpenedBy() + " (z-score " + score + ").";
        } else if (deviation.compareTo(new BigDecimal("20000")) > 0) {
            severity = CashAnomalySeverity.LOW;
            score = deviation.setScale(2, RoundingMode.HALF_UP);
            description = "Écart de caisse inhabituel de " + variance + " constaté pour le caissier "
                    + session.getOpenedBy() + " sans historique suffisant pour comparaison statistique.";
        } else {
            return;
        }
        flag(session, null, CashAnomalyType.VARIANCE_OUTLIER, severity, score, description, newFlags);
    }

    private void detectRoundNumberPattern(CashSession session, List<CashMovement> movements, List<CashAnomalyFlag> newFlags) {
        List<CashMovement> manual = movements.stream()
                .filter(m -> m.getAmount() != null && m.getAmount().signum() > 0)
                .toList();
        if (manual.size() < 3) {
            return;
        }
        long roundCount = manual.stream()
                .filter(m -> m.getAmount().remainder(ROUND_NUMBER_DIVISOR).signum() == 0)
                .count();
        BigDecimal ratio = BigDecimal.valueOf(roundCount).divide(BigDecimal.valueOf(manual.size()), 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(ROUND_PATTERN_RATIO_THRESHOLD) <= 0) {
            return;
        }
        CashAnomalySeverity severity = ratio.compareTo(new BigDecimal("0.8")) >= 0 ? CashAnomalySeverity.MEDIUM : CashAnomalySeverity.LOW;
        String description = roundCount + " des " + manual.size() + " mouvements de la session ("
                + ratio.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP)
                + " %) portent des montants ronds, ce qui peut indiquer des saisies fabriquées plutôt que réelles.";
        flag(session, null, CashAnomalyType.ROUND_NUMBER_PATTERN, severity, ratio.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP), description, newFlags);
    }

    private void detectExcessiveRefunds(CashSession session, List<CashMovement> movements, List<CashAnomalyFlag> newFlags) {
        long paymentCount = movements.stream().filter(m -> m.getMovementType() == com.sni.bokaticowork.features.payment.enums.CashMovementType.PAYMENT).count();
        long refundCount = movements.stream().filter(m -> m.getMovementType() == com.sni.bokaticowork.features.payment.enums.CashMovementType.REFUND).count();
        if (refundCount < 3 || paymentCount == 0) {
            return;
        }
        BigDecimal ratio = BigDecimal.valueOf(refundCount).divide(BigDecimal.valueOf(paymentCount), 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(EXCESSIVE_REFUND_RATIO_THRESHOLD) <= 0) {
            return;
        }
        CashAnomalySeverity severity = ratio.compareTo(new BigDecimal("0.6")) >= 0 ? CashAnomalySeverity.HIGH : CashAnomalySeverity.MEDIUM;
        String description = refundCount + " remboursements pour " + paymentCount + " paiements sur la session, soit un ratio de "
                + ratio.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP)
                + " % · taux de remboursement anormalement élevé.";
        flag(session, null, CashAnomalyType.EXCESSIVE_REFUNDS, severity, ratio.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP), description, newFlags);
    }

    private void detectExcessiveAdjustments(CashSession session, List<CashMovement> movements, List<CashAnomalyFlag> newFlags) {
        long adjustmentCount = movements.stream().filter(m -> m.getMovementType() == com.sni.bokaticowork.features.payment.enums.CashMovementType.ADJUSTMENT).count();
        if (adjustmentCount < 5) {
            return;
        }
        CashAnomalySeverity severity = adjustmentCount >= 10 ? CashAnomalySeverity.HIGH : CashAnomalySeverity.MEDIUM;
        String description = adjustmentCount + " ajustements manuels enregistrés sur cette session, ce qui dépasse le seuil habituel et mérite une revue.";
        flag(session, null, CashAnomalyType.EXCESSIVE_ADJUSTMENTS, severity, BigDecimal.valueOf(adjustmentCount), description, newFlags);
    }

    private void detectOffHoursSession(CashSession session, List<CashAnomalyFlag> newFlags) {
        if (session.getOpenedAt() == null) {
            return;
        }
        ZonedDateTime openedAt = session.getOpenedAt().atZone(ZoneOffset.UTC);
        int hour = openedAt.getHour();
        boolean offHours = hour >= OFF_HOURS_START || hour < OFF_HOURS_END;
        if (!offHours) {
            return;
        }
        String description = "Session de caisse ouverte en dehors des heures habituelles d'activité, à " + hour + "h (UTC), par "
                + session.getOpenedBy() + ".";
        flag(session, null, CashAnomalyType.OFF_HOURS_SESSION, CashAnomalySeverity.LOW, BigDecimal.valueOf(hour), description, newFlags);
    }

    private void detectThresholdStructuring(CashSession session, List<CashMovement> movements, List<CashAnomalyFlag> newFlags) {
        BigDecimal maxCashAmount = session.getCashRegister() == null ? null : session.getCashRegister().getMaxCashAmount();
        if (maxCashAmount == null || maxCashAmount.signum() <= 0) {
            return;
        }
        BigDecimal lowerBound = maxCashAmount.multiply(STRUCTURING_LOWER_RATIO);
        List<CashMovement> nearLimit = movements.stream()
                .filter(m -> m.getAmount() != null
                        && m.getAmount().compareTo(lowerBound) >= 0
                        && m.getAmount().compareTo(maxCashAmount) < 0)
                .toList();
        if (nearLimit.size() < 3) {
            return;
        }
        CashAnomalySeverity severity = nearLimit.size() >= 5 ? CashAnomalySeverity.HIGH : CashAnomalySeverity.MEDIUM;
        String description = nearLimit.size() + " mouvements avec des montants juste en dessous du plafond de caisse ("
                + maxCashAmount + "), ce qui peut indiquer un fractionnement délibéré pour éviter les contrôles (structuring).";
        flag(session, nearLimit.get(nearLimit.size() - 1), CashAnomalyType.THRESHOLD_STRUCTURING, severity,
                BigDecimal.valueOf(nearLimit.size()), description, newFlags);
    }

    private void detectNearMaxCashRecurrence(CashSession session, List<CashMovement> movements, List<CashAnomalyFlag> newFlags) {
        BigDecimal maxCashAmount = session.getCashRegister() == null ? null : session.getCashRegister().getMaxCashAmount();
        if (maxCashAmount == null || maxCashAmount.signum() <= 0) {
            return;
        }
        BigDecimal nearMaxThreshold = maxCashAmount.multiply(NEAR_MAX_RATIO);
        List<CashMovement> nearMax = movements.stream()
                .filter(m -> m.getRunningBalance() != null && m.getRunningBalance().compareTo(nearMaxThreshold) >= 0)
                .toList();
        if (nearMax.size() < 2) {
            return;
        }
        CashAnomalySeverity severity = nearMax.size() >= 4 ? CashAnomalySeverity.HIGH : CashAnomalySeverity.MEDIUM;
        String description = "Le solde de caisse a atteint ou dépassé " + NEAR_MAX_RATIO.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP)
                + " % du plafond autorisé (" + maxCashAmount + ") à " + nearMax.size()
                + " reprises au cours de la session, sans dépôt en coffre déclenché.";
        flag(session, nearMax.get(nearMax.size() - 1), CashAnomalyType.NEAR_MAX_CASH_RECURRENCE, severity,
                BigDecimal.valueOf(nearMax.size()), description, newFlags);
    }

    private void detectMaxCashOverflow(CashSession session, List<CashMovement> movements) {
        CashRegister register = session.getCashRegister();
        BigDecimal maxCashAmount = register == null ? null : register.getMaxCashAmount();
        if (register == null || maxCashAmount == null || maxCashAmount.signum() <= 0) {
            return;
        }
        CashMovement overflow = movements.stream()
                .filter(m -> m.getRunningBalance() != null && m.getRunningBalance().compareTo(maxCashAmount) > 0)
                .reduce((first, second) -> second)
                .orElse(null);
        if (overflow == null) {
            return;
        }
        Instant lastSent = register.getLastAnomalyAlertSentAt();
        if (lastSent != null && Instant.now().isBefore(lastSent.plus(alertCooldownHours, ChronoUnit.HOURS))) {
            return;
        }
        String description = "Le solde de la caisse " + register.getName() + " a dépassé le plafond autorisé ("
                + maxCashAmount + ") · solde constaté : " + overflow.getRunningBalance()
                + " lors de la session " + session.getSessionNumber() + ".";
        emailNotifier.notifyRegisterManager(
                register,
                "Dépassement du plafond de caisse · " + register.getRegisterCode(),
                description,
                session.getSessionNumber(),
                CASH_ANOMALIES_PATH + session.getSessionNumber()
        );
        register.setLastAnomalyAlertSentAt(Instant.now());
        cashRegisterRepository.save(register);
    }

    // ---- Persistence helpers ----

    private void flag(CashSession session, CashMovement movement, CashAnomalyType type, CashAnomalySeverity severity,
                      BigDecimal score, String description, List<CashAnomalyFlag> newFlags) {
        if (anomalyFlagRepository.existsByCashSession_IdAndAnomalyType(session.getId(), type)) {
            return;
        }
        CashAnomalyFlag savedFlag = anomalyFlagRepository.save(CashAnomalyFlag.builder()
                .flagNumber(sequenceGenerator.next("cash_anomaly_flag"))
                .cashSession(session)
                .cashMovement(movement)
                .anomalyType(type)
                .severity(severity)
                .score(score)
                .description(description)
                .status(CashAnomalyStatus.OPEN)
                .build());
        newFlags.add(savedFlag);

        if (severity == CashAnomalySeverity.MEDIUM || severity == CashAnomalySeverity.HIGH) {
            emailNotifier.notifySupervisor(
                    "Anomalie de caisse détectée · " + savedFlag.getFlagNumber(),
                    "Une anomalie de type " + type.name() + " (sévérité " + severity.name() + ") a été détectée sur la session "
                            + session.getSessionNumber() + " du caissier " + session.getOpenedBy() + ". " + description,
                    savedFlag.getFlagNumber(),
                    CASH_ANOMALIES_PATH + savedFlag.getFlagNumber()
            );
        }
        if (severity == CashAnomalySeverity.HIGH) {
            createTicketForAnomaly(session, savedFlag, description);
        }
    }

    private void createTicketForAnomaly(CashSession session, CashAnomalyFlag flag, String description) {
        try {
            supportTicketService.createFromAutomation(new CreateTicketRequest(
                    "Anomalie de caisse à risque élevé · " + flag.getFlagNumber(),
                    description,
                    TicketPriority.HIGH,
                    TicketCategory.BILLING,
                    "CASHIER",
                    session.getOpenedBy(),
                    null, null, null,
                    "CASH_ANOMALY_FLAG",
                    flag.getFlagNumber()
            ));
        } catch (Exception ex) {
            log.warn("Failed to create support ticket for cash anomaly {}", flag.getFlagNumber(), ex);
        }
    }

    private CashAnomalyFlagResponse toResponse(CashAnomalyFlag flag) {
        CashSession session = flag.getCashSession();
        CashMovement movement = flag.getCashMovement();
        return new CashAnomalyFlagResponse(
                flag.getFlagNumber(),
                session == null ? null : session.getSessionNumber(),
                movement == null ? null : movement.getMovementNumber(),
                session == null || session.getCashRegister() == null ? null : session.getCashRegister().getRegisterCode(),
                flag.getAnomalyType(),
                flag.getSeverity(),
                flag.getScore(),
                flag.getDescription(),
                flag.getDetectedAt(),
                flag.getStatus(),
                flag.getReviewedBy(),
                flag.getReviewedAt(),
                flag.getReviewNote()
        );
    }

    private CashSession session(String sessionNumber) {
        if (!StringUtils.hasText(sessionNumber)) {
            throw new BadRequestException("Cash session number is required");
        }
        return sessionRepository.findBySessionNumber(sessionNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Cash session not found"));
    }

    private CashAnomalyFlag find(String flagNumber) {
        if (!StringUtils.hasText(flagNumber)) {
            throw new BadRequestException("Anomaly flag number is required");
        }
        return anomalyFlagRepository.findByFlagNumber(flagNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Anomaly flag not found: " + flagNumber));
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(value.toString());
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
