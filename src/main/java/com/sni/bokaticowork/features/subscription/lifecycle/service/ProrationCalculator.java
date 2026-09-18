package com.sni.bokaticowork.features.subscription.lifecycle.service;

import com.sni.bokaticowork.features.subscription.lifecycle.model.ProrationPolicy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Le prorata, explicite et unique.
 *
 * <p>Une periode, un montant pour cette periode, un intervalle a l'interieur · la part du montant
 * qui correspond a l'intervalle. Le meme calcul sert a l'entree (les jours avant la premiere
 * echeance), a la sortie (les jours au-dela de la periode payee), au changement de plan (le
 * reste a courir a l'ancien et au nouveau prix) et au gel (les jours geles). Trois politiques :
 * au jour, au mois entame, aucune.</p>
 */
@Component
public class ProrationCalculator {

    private static final int SCALE = 4;

    /**
     * Part du montant de la periode {@code [periodStart, periodEnd]} qui revient a {@code [from, to]}.
     *
     * @return jamais negatif · zero si l'intervalle est vide ou hors de la periode
     */
    public BigDecimal share(BigDecimal periodAmount, LocalDate periodStart, LocalDate periodEnd,
                            LocalDate from, LocalDate to, ProrationPolicy policy) {
        if (periodAmount == null || periodAmount.signum() <= 0 || periodStart == null || periodEnd == null
                || from == null || to == null || to.isBefore(from)) {
            return BigDecimal.ZERO;
        }
        LocalDate start = from.isBefore(periodStart) ? periodStart : from;
        LocalDate end = to.isAfter(periodEnd) ? periodEnd : to;
        if (end.isBefore(start)) {
            return BigDecimal.ZERO;
        }
        return switch (policy == null ? ProrationPolicy.DAILY : policy) {
            case NONE -> periodAmount.setScale(SCALE, RoundingMode.HALF_UP);
            case DAILY -> {
                long periodDays = ChronoUnit.DAYS.between(periodStart, periodEnd) + 1;
                long days = ChronoUnit.DAYS.between(start, end) + 1;
                yield periodAmount.multiply(BigDecimal.valueOf(days))
                        .divide(BigDecimal.valueOf(periodDays), SCALE, RoundingMode.HALF_UP);
            }
            case MONTH_STARTED -> {
                long periodMonths = Math.max(1, monthsStarted(periodStart, periodEnd));
                long months = Math.max(1, monthsStarted(start, end));
                yield periodAmount.multiply(BigDecimal.valueOf(Math.min(months, periodMonths)))
                        .divide(BigDecimal.valueOf(periodMonths), SCALE, RoundingMode.HALF_UP);
            }
        };
    }

    /** Le complement · ce qui n'est pas consomme entre {@code from} et la fin de la periode. */
    public BigDecimal remaining(BigDecimal periodAmount, LocalDate periodStart, LocalDate periodEnd,
                                LocalDate from, ProrationPolicy policy) {
        if (policy == ProrationPolicy.NONE) {
            return BigDecimal.ZERO;
        }
        return share(periodAmount, periodStart, periodEnd, from, periodEnd, policy);
    }

    /** Nombre de mois entames entre deux dates incluses · le 15 janvier au 2 fevrier en compte deux. */
    static long monthsStarted(LocalDate from, LocalDate to) {
        return ChronoUnit.MONTHS.between(from.withDayOfMonth(1), to.withDayOfMonth(1)) + 1;
    }
}
