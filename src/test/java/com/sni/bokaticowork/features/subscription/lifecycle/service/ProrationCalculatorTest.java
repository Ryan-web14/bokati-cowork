package com.sni.bokaticowork.features.subscription.lifecycle.service;

import com.sni.bokaticowork.features.subscription.lifecycle.model.ProrationPolicy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Un seul calcul de prorata pour tout le module.
 *
 * <p>Si l'entrée, la sortie et le changement de plan divergeaient sur la façon de compter une
 * période entamée, le client paierait trois fois des choses différentes pour les mêmes jours.</p>
 */
class ProrationCalculatorTest {

    private final ProrationCalculator calculator = new ProrationCalculator();
    private final LocalDate start = LocalDate.of(2026, 3, 1);
    private final LocalDate end = LocalDate.of(2026, 3, 31);
    private final BigDecimal month = new BigDecimal("31000");

    @Test
    void dailyCountsEachDayAtTheSamePrice() {
        BigDecimal share = calculator.share(month, start, end, LocalDate.of(2026, 3, 22), end, ProrationPolicy.DAILY);
        assertEquals(0, new BigDecimal("10000").compareTo(share), "10 jours sur 31 à 1000 le jour");
    }

    @Test
    void monthStartedCountsAnyStartedMonthInFull() {
        LocalDate quarterEnd = LocalDate.of(2026, 5, 31);
        BigDecimal quarter = new BigDecimal("90000");
        BigDecimal share = calculator.share(quarter, start, quarterEnd, LocalDate.of(2026, 4, 28), quarterEnd, ProrationPolicy.MONTH_STARTED);
        assertEquals(0, new BigDecimal("60000").compareTo(share), "avril entamé et mai · deux mois sur trois");
    }

    @Test
    void noneMeansTheWholePeriodIsDueAndNothingIsReturned() {
        assertEquals(0, month.compareTo(calculator.share(month, start, end, LocalDate.of(2026, 3, 30), end, ProrationPolicy.NONE)));
        assertEquals(0, BigDecimal.ZERO.compareTo(calculator.remaining(month, start, end, LocalDate.of(2026, 3, 2), ProrationPolicy.NONE)));
    }

    @Test
    void intervalOutsideThePeriodIsZeroAndIntervalIsClampedToIt() {
        assertEquals(0, BigDecimal.ZERO.compareTo(calculator.share(month, start, end, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 5), ProrationPolicy.DAILY)));
        assertEquals(0, month.compareTo(calculator.share(month, start, end, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 4, 30), ProrationPolicy.DAILY)));
        assertEquals(0, BigDecimal.ZERO.compareTo(calculator.share(month, start, end, end, start, ProrationPolicy.DAILY)));
    }

    @Test
    void remainingIsWhatIsNotConsumedFromADate() {
        BigDecimal remaining = calculator.remaining(month, start, end, LocalDate.of(2026, 3, 31), ProrationPolicy.DAILY);
        assertEquals(0, new BigDecimal("1000").compareTo(remaining));
    }

    @Test
    void monthsStartedCountsCalendarMonthsTouched() {
        assertEquals(2, ProrationCalculator.monthsStarted(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 2)));
        assertEquals(1, ProrationCalculator.monthsStarted(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)));
    }
}
