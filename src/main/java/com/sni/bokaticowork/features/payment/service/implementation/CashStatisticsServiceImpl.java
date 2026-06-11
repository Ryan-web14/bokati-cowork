package com.sni.bokaticowork.features.payment.service.implementation;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.payment.dto.response.CashRegisterStatisticsResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashSessionStatisticsResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashierStatisticsResponse;
import com.sni.bokaticowork.features.payment.model.CashMovement;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.repository.CashAnomalyFlagRepository;
import com.sni.bokaticowork.features.payment.repository.CashMovementRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterRepository;
import com.sni.bokaticowork.features.payment.repository.CashSessionRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.CashStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CashStatisticsServiceImpl implements CashStatisticsService {

    private static final int MONEY_SCALE = 4;

    private final CashSessionRepository sessionRepository;
    private final CashMovementRepository movementRepository;
    private final CashRegisterRepository registerRepository;
    private final CashAnomalyFlagRepository anomalyFlagRepository;

    @Override
    public CashSessionStatisticsResponse sessionStatistics(String sessionNumber) {
        CashSession session = sessionRepository.findBySessionNumber(sessionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Session de caisse introuvable : " + sessionNumber));
        List<CashMovement> movements = movementRepository.findByCashSession_IdOrderByCreatedAtAsc(session.getId());

        Instant openedAt = session.getOpenedAt();
        Instant closedAt = session.getClosedAt();
        Instant referenceEnd = closedAt != null ? closedAt : Instant.now();
        long durationMinutes = openedAt != null ? Math.max(0, Duration.between(openedAt, referenceEnd).toMinutes()) : 0;

        long movementCount = movements.size();
        BigDecimal hours = BigDecimal.valueOf(durationMinutes).divide(BigDecimal.valueOf(60), MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal transactionsPerHour = hours.signum() > 0
                ? BigDecimal.valueOf(movementCount).divide(hours, MONEY_SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal totalInflow = BigDecimal.ZERO;
        BigDecimal totalOutflow = BigDecimal.ZERO;
        BigDecimal amountSum = BigDecimal.ZERO;
        for (CashMovement movement : movements) {
            BigDecimal amount = safeAmount(movement.getAmount());
            amountSum = amountSum.add(amount);
            switch (movement.getMovementType()) {
                case PAYMENT, CASH_IN, TRANSFER_IN, OPENING_FLOAT -> totalInflow = totalInflow.add(amount);
                case REFUND, CASH_OUT, SAFE_DEPOSIT, TRANSFER_OUT -> totalOutflow = totalOutflow.add(amount);
                case ADJUSTMENT, CLOSING_COUNT -> { }
            }
        }
        BigDecimal inOutRatio = totalOutflow.signum() > 0
                ? totalInflow.divide(totalOutflow, MONEY_SCALE, RoundingMode.HALF_UP)
                : null;
        BigDecimal averageMovementAmount = movementCount > 0
                ? amountSum.divide(BigDecimal.valueOf(movementCount), MONEY_SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long idleMinutes = computeIdleMinutes(movements, openedAt, referenceEnd);

        BigDecimal varianceAmount = session.getVarianceAmount();
        BigDecimal expected = session.getExpectedClosingAmount();
        BigDecimal variancePercentage = (varianceAmount != null && expected != null && expected.signum() != 0)
                ? varianceAmount.divide(expected, MONEY_SCALE, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : null;

        return new CashSessionStatisticsResponse(
                session.getSessionNumber(),
                session.getCashRegister() != null ? session.getCashRegister().getRegisterCode() : null,
                session.getOpenedBy(),
                openedAt,
                closedAt,
                durationMinutes,
                movementCount,
                transactionsPerHour,
                totalInflow.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                totalOutflow.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                inOutRatio,
                idleMinutes,
                averageMovementAmount,
                varianceAmount,
                variancePercentage
        );
    }

    @Override
    public CashierStatisticsResponse cashierStatistics(String cashierCode) {
        List<CashSession> sessions = sessionRepository.findByOpenedByOrderByOpenedAtDesc(cashierCode);

        long sessionCount = sessions.size();
        long closedSessionCount = sessions.stream().filter(s -> s.getClosedAt() != null).count();
        long reviewSessionCount = sessions.stream()
                .filter(s -> s.getVarianceAmount() != null && s.getVarianceAmount().signum() != 0)
                .count();
        BigDecimal reviewFrequencyPercentage = sessionCount > 0
                ? BigDecimal.valueOf(reviewSessionCount)
                        .divide(BigDecimal.valueOf(sessionCount), MONEY_SCALE, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        BigDecimal averageSessionDurationMinutes = averageDurationMinutes(sessions);

        Object[] varianceStats = anomalyFlagRepository.cashierVarianceStats(cashierCode);
        BigDecimal averageVarianceAmount = BigDecimal.ZERO;
        BigDecimal varianceStandardDeviation = BigDecimal.ZERO;
        if (varianceStats != null && varianceStats.length >= 2) {
            averageVarianceAmount = toBigDecimal(varianceStats[0]);
            varianceStandardDeviation = toBigDecimal(varianceStats[1]);
        }

        String varianceTrend = computeVarianceTrend(sessions);
        BigDecimal riskScore = computeRiskScore(reviewFrequencyPercentage, varianceStandardDeviation, varianceTrend);

        return new CashierStatisticsResponse(
                cashierCode,
                sessionCount,
                closedSessionCount,
                reviewSessionCount,
                reviewFrequencyPercentage,
                averageVarianceAmount,
                varianceStandardDeviation,
                varianceTrend,
                averageSessionDurationMinutes,
                riskScore
        );
    }

    @Override
    public CashRegisterStatisticsResponse registerStatistics(String registerCode, Instant fromDate, Instant toDate) {
        CashRegister register = registerRepository.findByRegisterCode(registerCode)
                .orElseThrow(() -> new ResourceNotFoundException("Caisse introuvable : " + registerCode));

        List<CashSession> currentSessions = sessionRepository.findByRegisterAndPeriod(register.getId(), fromDate, toDate);
        List<CashMovement> currentMovements = currentSessions.stream()
                .flatMap(s -> movementRepository.findByCashSession_IdOrderByCreatedAtAsc(s.getId()).stream())
                .toList();

        long sessionCount = currentSessions.size();
        long movementCount = currentMovements.size();
        BigDecimal averageSessionDurationMinutes = averageDurationMinutes(currentSessions);
        BigDecimal averageVarianceAmount = averageVariance(currentSessions);
        List<CashRegisterStatisticsResponse.HourlyVolume> peakHours = computePeakHours(currentMovements);
        BigDecimal periodOverPeriodChangePercentage = computePeriodOverPeriodChange(register, fromDate, toDate, movementCount);

        return new CashRegisterStatisticsResponse(
                register.getRegisterCode(),
                register.getName(),
                sessionCount,
                movementCount,
                averageSessionDurationMinutes,
                averageVarianceAmount,
                peakHours,
                periodOverPeriodChangePercentage
        );
    }

    private long computeIdleMinutes(List<CashMovement> movements, Instant openedAt, Instant referenceEnd) {
        if (movements.isEmpty()) {
            return openedAt != null ? Math.max(0, Duration.between(openedAt, referenceEnd).toMinutes()) : 0;
        }
        long idle = 0;
        Instant cursor = openedAt;
        for (CashMovement movement : movements) {
            if (cursor != null && movement.getCreatedAt() != null) {
                idle += Math.max(0, Duration.between(cursor, movement.getCreatedAt()).toMinutes());
            }
            cursor = movement.getCreatedAt();
        }
        if (cursor != null) {
            idle += Math.max(0, Duration.between(cursor, referenceEnd).toMinutes());
        }
        return idle;
    }

    private BigDecimal averageDurationMinutes(List<CashSession> sessions) {
        List<Long> durations = sessions.stream()
                .filter(s -> s.getOpenedAt() != null && s.getClosedAt() != null)
                .map(s -> Duration.between(s.getOpenedAt(), s.getClosedAt()).toMinutes())
                .toList();
        if (durations.isEmpty()) {
            return BigDecimal.ZERO;
        }
        long sum = durations.stream().mapToLong(Long::longValue).sum();
        return BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(durations.size()), MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal averageVariance(List<CashSession> sessions) {
        List<BigDecimal> variances = sessions.stream()
                .map(CashSession::getVarianceAmount)
                .filter(v -> v != null)
                .toList();
        if (variances.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = variances.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(variances.size()), MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private String computeVarianceTrend(List<CashSession> sessions) {
        List<BigDecimal> variances = sessions.stream()
                .filter(s -> s.getClosedAt() != null && s.getVarianceAmount() != null)
                .sorted(Comparator.comparing(CashSession::getClosedAt))
                .map(s -> s.getVarianceAmount().abs())
                .toList();
        if (variances.size() < 4) {
            return "STABLE";
        }
        int half = variances.size() / 2;
        BigDecimal olderAverage = average(variances.subList(0, half));
        BigDecimal recentAverage = average(variances.subList(half, variances.size()));
        if (olderAverage.signum() == 0) {
            return recentAverage.signum() > 0 ? "DEGRADING" : "STABLE";
        }
        BigDecimal changeRatio = recentAverage.subtract(olderAverage)
                .divide(olderAverage, MONEY_SCALE, RoundingMode.HALF_UP);
        if (changeRatio.compareTo(new BigDecimal("0.10")) > 0) {
            return "DEGRADING";
        }
        if (changeRatio.compareTo(new BigDecimal("-0.10")) < 0) {
            return "IMPROVING";
        }
        return "STABLE";
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal computeRiskScore(BigDecimal reviewFrequencyPercentage, BigDecimal varianceStandardDeviation, String varianceTrend) {
        BigDecimal score = reviewFrequencyPercentage.multiply(new BigDecimal("0.5"))
                .add(varianceStandardDeviation.min(new BigDecimal("1000")).multiply(new BigDecimal("0.05")));
        if ("DEGRADING".equals(varianceTrend)) {
            score = score.add(BigDecimal.TEN);
        }
        return score.min(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }

    private List<CashRegisterStatisticsResponse.HourlyVolume> computePeakHours(List<CashMovement> movements) {
        Map<Integer, List<CashMovement>> byHour = movements.stream()
                .filter(m -> m.getCreatedAt() != null)
                .collect(Collectors.groupingBy(m -> m.getCreatedAt().atZone(ZoneOffset.UTC).getHour()));

        return byHour.entrySet().stream()
                .map(entry -> new CashRegisterStatisticsResponse.HourlyVolume(
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue().stream()
                                .map(m -> safeAmount(m.getAmount()))
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(MONEY_SCALE, RoundingMode.HALF_UP)))
                .sorted(Comparator.comparingLong(CashRegisterStatisticsResponse.HourlyVolume::movementCount).reversed())
                .limit(5)
                .toList();
    }

    private BigDecimal computePeriodOverPeriodChange(CashRegister register, Instant fromDate, Instant toDate, long currentMovementCount) {
        if (fromDate == null || toDate == null) {
            return null;
        }
        Duration span = Duration.between(fromDate, toDate);
        if (span.isZero() || span.isNegative()) {
            return null;
        }
        Instant previousFrom = fromDate.minus(span);
        Instant previousTo = fromDate;
        List<CashSession> previousSessions = sessionRepository.findByRegisterAndPeriod(register.getId(), previousFrom, previousTo);
        long previousMovementCount = previousSessions.stream()
                .mapToLong(s -> movementRepository.findByCashSession_IdOrderByCreatedAtAsc(s.getId()).size())
                .sum();
        if (previousMovementCount == 0) {
            return null;
        }
        return BigDecimal.valueOf(currentMovementCount - previousMovementCount)
                .divide(BigDecimal.valueOf(previousMovementCount), MONEY_SCALE, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        return new BigDecimal(value.toString()).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
