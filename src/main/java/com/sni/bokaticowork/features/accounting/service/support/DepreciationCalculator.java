package com.sni.bokaticowork.features.accounting.service.support;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Calculs des immobilisations (lot F18), sans etat : plans lineaire (prorata temporis au jour,
 * annee de 360 jours ou jours reels) et degressif (coefficient, bascule en lineaire), part d'un
 * mois jusqu'a une date de sortie, plus ou moins-value, regularisation de la TVA deduite.
 *
 * <p>Les dotations mensuelles sont arrondies au franc sur le cumul : la somme des dotations egale
 * toujours la base, sans derive d'arrondi.</p>
 */
public final class DepreciationCalculator {

    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);

    private DepreciationCalculator() {
    }

    /** Dotation d'un mois ({@code AAAA-MM}). */
    public record Line(String period, BigDecimal amount) {
    }

    public static String period(YearMonth month) {
        return month.toString();
    }

    /** Jours entre deux dates (fin exclue) : base 30/360, ou jours reels. */
    public static long days(LocalDate from, LocalDate to, boolean days360) {
        if (!to.isAfter(from)) {
            return 0;
        }
        if (!days360) {
            return ChronoUnit.DAYS.between(from, to);
        }
        int d1 = Math.min(from.getDayOfMonth(), 30);
        int d2 = Math.min(to.getDayOfMonth(), 30);
        return (long) (to.getYear() - from.getYear()) * 360 + (long) (to.getMonthValue() - from.getMonthValue()) * 30 + (d2 - d1);
    }

    /**
     * Plan lineaire : la base est repartie au prorata des jours de chaque mois entre le debut
     * (inclus) et la fin (exclue). Mise en service le 1er avril sur 36 mois : 36 dotations egales.
     */
    public static List<Line> linear(BigDecimal base, LocalDate start, LocalDate end, boolean days360) {
        List<Line> result = new ArrayList<>();
        BigDecimal amount = scale(base);
        if (amount.signum() <= 0) {
            return result;
        }
        if (!end.isAfter(start)) {
            result.add(new Line(period(YearMonth.from(start)), amount));
            return result;
        }
        long total = days(start, end, days360);
        long cumulativeDays = 0;
        BigDecimal previous = BigDecimal.ZERO;
        YearMonth month = YearMonth.from(start);
        while (month.atDay(1).isBefore(end)) {
            LocalDate from = start.isAfter(month.atDay(1)) ? start : month.atDay(1);
            LocalDate next = month.plusMonths(1).atDay(1);
            LocalDate to = end.isBefore(next) ? end : next;
            cumulativeDays += days(from, to, days360);
            boolean last = !next.isBefore(end);
            BigDecimal cumulative = last ? amount
                    : amount.multiply(BigDecimal.valueOf(cumulativeDays)).divide(BigDecimal.valueOf(total), 0, RoundingMode.HALF_UP);
            BigDecimal part = cumulative.subtract(previous);
            if (part.signum() != 0 || last) {
                result.add(new Line(period(month), part));
            }
            previous = cumulative;
            month = month.plusMonths(1);
        }
        return result;
    }

    /**
     * Plan degressif : taux lineaire multiplie par le coefficient, applique a la valeur nette de
     * chaque exercice (annee civile) a partir du premier jour du mois de depart ; bascule en
     * lineaire quand l'annuite lineaire sur la duree restante devient superieure. La derniere
     * annee solde la valeur nette.
     */
    public static List<Line> declining(BigDecimal base, YearMonth start, int months, BigDecimal annualRate) {
        List<Line> result = new ArrayList<>();
        BigDecimal net = scale(base);
        if (net.signum() <= 0 || months <= 0) {
            return result;
        }
        int remaining = months;
        YearMonth cursor = start;
        while (remaining > 0) {
            int inYear = Math.min(remaining, 13 - cursor.getMonthValue());
            BigDecimal yearAmount;
            if (inYear == remaining) {
                yearAmount = net;
            } else {
                BigDecimal declining = net.multiply(annualRate).multiply(BigDecimal.valueOf(inYear)).divide(TWELVE, 0, RoundingMode.HALF_UP);
                BigDecimal linear = net.multiply(BigDecimal.valueOf(inYear)).divide(BigDecimal.valueOf(remaining), 0, RoundingMode.HALF_UP);
                yearAmount = declining.max(linear).min(net);
            }
            BigDecimal previous = BigDecimal.ZERO;
            for (int index = 1; index <= inYear; index++) {
                BigDecimal cumulative = index == inYear ? yearAmount
                        : yearAmount.multiply(BigDecimal.valueOf(index)).divide(BigDecimal.valueOf(inYear), 0, RoundingMode.HALF_UP);
                result.add(new Line(period(cursor.plusMonths(index - 1)), cumulative.subtract(previous)));
                previous = cumulative;
            }
            net = net.subtract(yearAmount);
            remaining -= inYear;
            cursor = cursor.plusMonths(inYear);
        }
        return result;
    }

    /** Duree en mois d'un taux lineaire annuel en pourcentage : 15 % donne 80 mois, 20 % donne 60 mois. */
    public static int monthsForRate(BigDecimal annualRate) {
        if (annualRate == null || annualRate.signum() <= 0) {
            throw new IllegalArgumentException("Taux annuel positif attendu");
        }
        return BigDecimal.valueOf(1200).divide(annualRate, 0, RoundingMode.HALF_UP).intValueExact();
    }

    /** Taux degressif annuel : coefficient divise par la duree en annees. */
    public static BigDecimal decliningRate(int durationMonths, BigDecimal coefficient) {
        return coefficient.multiply(TWELVE).divide(BigDecimal.valueOf(durationMonths), MathContext.DECIMAL64);
    }

    /**
     * Coefficient du degressif selon la duree, depuis une table parametree
     * ({@code 3-4:1.5,5-6:2,7+:2.5}) ; nul si la duree n'en a pas (moins de 3 ans).
     */
    public static BigDecimal coefficient(int durationMonths, String table) {
        double years = durationMonths / 12.0;
        for (String part : (table == null ? "" : table).split(",")) {
            String[] pair = part.trim().split(":");
            if (pair.length != 2) {
                continue;
            }
            try {
                String range = pair[0].trim();
                BigDecimal value = new BigDecimal(pair[1].trim());
                if (range.endsWith("+")) {
                    // « 7+ » couvre plus de 6 ans : une duree de 6 ans et 8 mois prend ce coefficient.
                    if (years > Double.parseDouble(range.substring(0, range.length() - 1)) - 1) {
                        return value;
                    }
                } else if (range.contains("-")) {
                    // « 5-6 » couvre plus de 4 ans et jusqu'a 6 ans inclus : les tranches se suivent sans trou.
                    String[] bounds = range.split("-");
                    if (years > Double.parseDouble(bounds[0]) - 1 && years <= Double.parseDouble(bounds[1])) {
                        return value;
                    }
                } else if (years == Double.parseDouble(range)) {
                    return value;
                }
            } catch (RuntimeException ignored) {
                // ligne de la table illisible : ignoree
            }
        }
        return null;
    }

    /** Part d'une dotation mensuelle courue du premier jour du mois jusqu'a {@code until} inclus. */
    public static BigDecimal prorata(BigDecimal monthAmount, LocalDate until, boolean days360) {
        YearMonth month = YearMonth.from(until);
        long full = days(month.atDay(1), month.plusMonths(1).atDay(1), days360);
        long elapsed = until.equals(month.atEndOfMonth()) ? full : days(month.atDay(1), until.plusDays(1), days360);
        if (full <= 0 || elapsed >= full) {
            return scale(monthAmount);
        }
        return monthAmount.multiply(BigDecimal.valueOf(elapsed)).divide(BigDecimal.valueOf(full), 0, RoundingMode.HALF_UP);
    }

    /** Plus-value (positive) ou moins-value (negative) : prix de cession moins valeur nette comptable. */
    public static BigDecimal gainOrLoss(BigDecimal price, BigDecimal netValue) {
        return scale(price).subtract(scale(netValue));
    }

    /**
     * TVA deduite a reverser quand le bien sort sans TVA (ou change d'affectation) avant la fin de
     * la periode de regularisation : la TVA deduite multipliee par les annees civiles restant a
     * courir, l'annee d'acquisition et celle de la sortie comptant pour entieres, sur la duree de
     * la periode.
     */
    public static BigDecimal vatRegularization(BigDecimal deductedVat, LocalDate acquisition, LocalDate event, int years) {
        if (deductedVat == null || deductedVat.signum() <= 0 || acquisition == null || event == null || years <= 0) {
            return BigDecimal.ZERO;
        }
        int elapsed = event.getYear() - acquisition.getYear() + 1;
        int remaining = Math.max(0, years - elapsed);
        return deductedVat.multiply(BigDecimal.valueOf(remaining)).divide(BigDecimal.valueOf(years), 0, RoundingMode.HALF_UP);
    }

    private static BigDecimal scale(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(0, RoundingMode.HALF_UP);
    }
}
