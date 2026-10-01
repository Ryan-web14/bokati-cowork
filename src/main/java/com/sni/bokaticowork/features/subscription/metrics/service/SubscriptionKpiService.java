package com.sni.bokaticowork.features.subscription.metrics.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.subscription.lifecycle.service.SubscriptionCommitmentService;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Les indicateurs qu'on ne pouvait pas dire : attrition, revenu recurrent, valeur vie, occupation.
 *
 * <p>Le revenu recurrent mensuel ramene tous les rythmes au mois · un abonnement annuel de 1 200 000
 * pese 100 000 par mois, pas zero parce qu'il n'est pas mensuel. L'attrition est le rapport entre
 * ce qui est parti sur la periode et ce qui etait la au debut. La valeur vie est l'ARPU divise par
 * l'attrition mensuelle · une estimation, dite comme telle.</p>
 */
@Service
@RequiredArgsConstructor
public class SubscriptionKpiService {

    private static final List<String> ENTITLED = List.of("ACTIVE", "TRIALING", "GRACE_PERIOD", "PAST_DUE", "PENDING_TERMINATION");

    @PersistenceContext
    private EntityManager entityManager;

    /** Codes des types de ressource qui comptent comme postes · vide pour toutes les ressources actives. */
    @Value("${bokati.subscription.kpi.workspace-type-codes:}")
    private String workspaceTypeCodes;

    public record Kpis(LocalDate from, LocalDate to,
                       long activeSubscriptions, BigDecimal mrr, BigDecimal arr, Map<String, BigDecimal> mrrByCycle,
                       long activeAtStart, long newInPeriod, long churnedInPeriod, BigDecimal churnRate, BigDecimal monthlyChurnRate,
                       BigDecimal newMrr, BigDecimal churnedMrr, BigDecimal netMrrMovement,
                       BigDecimal arpu, BigDecimal customerLifetimeValue, BigDecimal averageLifetimeMonths,
                       long seatsOccupied, long workspaceCapacity, BigDecimal occupancyRate, BigDecimal revenuePerSeat,
                       long inGracePeriod, long pendingTermination, long pendingDocuments) {
    }

    @Transactional(readOnly = true)
    public Kpis compute(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new BadRequestException("Indiquez une période · du et au, dans l'ordre");
        }
        List<Object[]> live = rows("SELECT billing_cycle, total_amount FROM subscription WHERE status IN (:statuses)", Map.of("statuses", ENTITLED));
        Map<String, BigDecimal> mrrByCycle = new TreeMap<>();
        BigDecimal mrr = BigDecimal.ZERO;
        for (Object[] row : live) {
            BigDecimal monthly = monthly((String) row[0], (BigDecimal) row[1]);
            mrr = mrr.add(monthly);
            mrrByCycle.merge(String.valueOf(row[0]), monthly, BigDecimal::add);
        }
        long active = live.size();

        var fromTs = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        var toTs = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        long activeAtStart = count("""
                SELECT count(*) FROM subscription
                WHERE created_at < :from AND status <> 'DRAFT'
                  AND (cancelled_at IS NULL OR cancelled_at >= :from)
                  AND NOT (status = 'EXPIRED' AND updated_at < :from)
                """, Map.of("from", fromTs));
        List<Object[]> created = rows("SELECT billing_cycle, total_amount FROM subscription WHERE created_at >= :from AND created_at < :to AND status <> 'DRAFT'",
                Map.of("from", fromTs, "to", toTs));
        List<Object[]> churned = rows("SELECT billing_cycle, total_amount FROM subscription WHERE cancelled_at >= :from AND cancelled_at < :to",
                Map.of("from", fromTs, "to", toTs));
        BigDecimal newMrr = created.stream().map(r -> monthly((String) r[0], (BigDecimal) r[1])).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal churnedMrr = churned.stream().map(r -> monthly((String) r[0], (BigDecimal) r[1])).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal churnRate = activeAtStart == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(churned.size()).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(activeAtStart), 2, RoundingMode.HALF_UP);
        long days = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;
        BigDecimal months = BigDecimal.valueOf(days).divide(BigDecimal.valueOf(30.4375), 6, RoundingMode.HALF_UP);
        BigDecimal monthlyChurn = months.signum() == 0 ? churnRate : churnRate.divide(months, 4, RoundingMode.HALF_UP);

        BigDecimal arpu = active == 0 ? BigDecimal.ZERO : mrr.divide(BigDecimal.valueOf(active), 2, RoundingMode.HALF_UP);
        BigDecimal lifetimeMonths = monthlyChurn.signum() == 0 ? null
                : BigDecimal.valueOf(100).divide(monthlyChurn, 1, RoundingMode.HALF_UP);
        BigDecimal clv = lifetimeMonths == null ? null : arpu.multiply(lifetimeMonths).setScale(2, RoundingMode.HALF_UP);

        long seats = count("SELECT count(*) FROM subscription_seat WHERE status = 'ACTIVE'", Map.of());
        long capacity = workspaceCapacity();
        BigDecimal occupancy = capacity == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(seats).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(capacity), 2, RoundingMode.HALF_UP);
        BigDecimal revenuePerSeat = seats == 0 ? BigDecimal.ZERO : mrr.divide(BigDecimal.valueOf(seats), 2, RoundingMode.HALF_UP);

        return new Kpis(from, to, active, scale(mrr), scale(mrr.multiply(BigDecimal.valueOf(12))), mrrByCycle,
                activeAtStart, created.size(), churned.size(), churnRate, monthlyChurn,
                scale(newMrr), scale(churnedMrr), scale(newMrr.subtract(churnedMrr)),
                arpu, clv, lifetimeMonths,
                seats, capacity, occupancy, revenuePerSeat,
                count("SELECT count(*) FROM subscription WHERE status = 'GRACE_PERIOD'", Map.of()),
                count("SELECT count(*) FROM subscription WHERE status = 'PENDING_TERMINATION'", Map.of()),
                count("SELECT count(*) FROM subscription WHERE status = 'PENDING_DOCUMENTS'", Map.of()));
    }

    private long workspaceCapacity() {
        List<String> codes = Arrays.stream(workspaceTypeCodes.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        if (codes.isEmpty()) {
            return count("SELECT coalesce(sum(capacity), 0) FROM resource WHERE active = true", Map.of());
        }
        return count("SELECT coalesce(sum(r.capacity), 0) FROM resource r JOIN resource_type t ON t.id = r.type_id WHERE r.active = true AND t.code IN (:codes)",
                Map.of("codes", codes));
    }

    static BigDecimal monthly(String cycle, BigDecimal total) {
        BillingCycle billingCycle;
        try {
            billingCycle = BillingCycle.valueOf(cycle);
        } catch (RuntimeException ex) {
            billingCycle = BillingCycle.MONTHLY;
        }
        return SubscriptionCommitmentService.monthlyEquivalent(Subscription.builder().billingCycle(billingCycle).totalAmount(total).build());
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> rows(String sql, Map<String, Object> params) {
        var query = entityManager.createNativeQuery(sql);
        params.forEach(query::setParameter);
        return query.getResultList();
    }

    private long count(String sql, Map<String, Object> params) {
        var query = entityManager.createNativeQuery(sql);
        params.forEach(query::setParameter);
        Object result = query.getSingleResult();
        return result == null ? 0 : ((Number) result).longValue();
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
