package com.sni.bokaticowork.features.accounting.service.support;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Tableau d'amortissement d'un emprunt (lot F17), arrondi au franc.
 *
 * <ul>
 *   <li>Modes : echeances constantes ({@code ANNUITY}), amortissement constant du capital
 *       ({@code CONSTANT_CAPITAL}), remboursement in fine ({@code BULLET}).</li>
 *   <li>Differe : {@code CAPITAL} (seuls les interets sont payes), {@code TOTAL} (les interets
 *       s'ajoutent au capital) sur les premieres echeances.</li>
 *   <li>Base : {@code 30_360} (taux annuel x mois / 12) ou {@code ACT_365} (jours reels / 365) ;
 *       l'echeance constante est calculee au taux periodique nominal.</li>
 * </ul>
 */
public final class LoanSchedule {

    private static final MathContext MC = MathContext.DECIMAL64;

    private LoanSchedule() {
    }

    public record Terms(BigDecimal balance, BigDecimal annualRate, String dayCount, int frequencyMonths, int count, String deferralType,
                        int deferralCount, String method, BigDecimal insurance, LocalDate previousDate, LocalDate firstDueDate, int firstNumber) {
    }

    public record Line(int number, LocalDate dueDate, BigDecimal openingBalance, BigDecimal interest, BigDecimal principal, BigDecimal insurance,
                       BigDecimal payment, BigDecimal closingBalance) {
    }

    public static List<Line> compute(Terms terms) {
        List<Line> lines = new ArrayList<>();
        BigDecimal balance = round(terms.balance());
        BigDecimal nominal = periodicRate(terms.annualRate(), terms.frequencyMonths());
        BigDecimal insurance = round(terms.insurance() == null ? BigDecimal.ZERO : terms.insurance());
        int deferral = "NONE".equals(terms.deferralType()) ? 0 : Math.min(terms.deferralCount(), Math.max(0, terms.count() - 1));
        LocalDate previous = terms.previousDate();
        BigDecimal annuity = null;
        BigDecimal constantPrincipal = null;
        for (int i = 0; i < terms.count(); i++) {
            LocalDate due = terms.firstDueDate().plusMonths((long) i * terms.frequencyMonths());
            BigDecimal rate = "ACT_365".equals(terms.dayCount()) && previous != null
                    ? terms.annualRate().divide(BigDecimal.valueOf(100), MC).multiply(BigDecimal.valueOf(ChronoUnit.DAYS.between(previous, due)), MC)
                    .divide(BigDecimal.valueOf(365), MC)
                    : nominal;
            BigDecimal opening = balance;
            BigDecimal interest = round(opening.multiply(rate, MC));
            BigDecimal principal;
            BigDecimal payment;
            boolean last = i == terms.count() - 1;
            if (i < deferral) {
                if ("TOTAL".equals(terms.deferralType())) {
                    // Differe total : rien n'est paye, les interets s'ajoutent au capital.
                    balance = balance.add(interest);
                    lines.add(new Line(terms.firstNumber() + i, due, opening, interest, interest.negate(), insurance, insurance, balance));
                    previous = due;
                    continue;
                }
                principal = BigDecimal.ZERO;
                payment = interest.add(insurance);
            } else {
                int remaining = terms.count() - i;
                switch (terms.method()) {
                    case "BULLET" -> principal = last ? balance : BigDecimal.ZERO;
                    case "CONSTANT_CAPITAL" -> {
                        if (constantPrincipal == null) {
                            constantPrincipal = round(balance.divide(BigDecimal.valueOf(remaining), MC));
                        }
                        principal = last ? balance : constantPrincipal.min(balance);
                    }
                    default -> {
                        if (annuity == null) {
                            annuity = annuity(balance, nominal, remaining);
                        }
                        principal = last ? balance : annuity.subtract(interest).max(BigDecimal.ZERO).min(balance);
                    }
                }
                payment = principal.add(interest).add(insurance);
            }
            balance = balance.subtract(principal);
            lines.add(new Line(terms.firstNumber() + i, due, opening, interest, principal, insurance, payment, balance));
            previous = due;
        }
        return lines;
    }

    /** Echeance constante hors assurance : B x r / (1 - (1 + r)^-n), ou B / n a taux nul. */
    static BigDecimal annuity(BigDecimal balance, BigDecimal rate, int count) {
        if (rate.signum() == 0) {
            return round(balance.divide(BigDecimal.valueOf(count), MC));
        }
        BigDecimal factor = BigDecimal.ONE.add(rate).pow(-count, MC);
        return round(balance.multiply(rate, MC).divide(BigDecimal.ONE.subtract(factor), MC));
    }

    /** Nombre d'echeances pour solder {@code balance} avec l'echeance {@code payment} (remboursement anticipe a duree reduite). */
    public static int termsFor(BigDecimal balance, BigDecimal annualRate, int frequencyMonths, BigDecimal payment) {
        BigDecimal rate = periodicRate(annualRate, frequencyMonths);
        if (payment.signum() <= 0) {
            return 1;
        }
        if (rate.signum() == 0) {
            return Math.max(1, balance.divide(payment, 0, RoundingMode.CEILING).intValue());
        }
        double r = rate.doubleValue();
        double x = 1 - balance.doubleValue() * r / payment.doubleValue();
        if (x <= 0) {
            throw new IllegalArgumentException("L'echeance ne couvre pas les interets");
        }
        return Math.max(1, (int) Math.ceil(-Math.log(x) / Math.log(1 + r) - 1e-9));
    }

    public static BigDecimal periodicRate(BigDecimal annualRate, int frequencyMonths) {
        return (annualRate == null ? BigDecimal.ZERO : annualRate).divide(BigDecimal.valueOf(100), MC)
                .multiply(BigDecimal.valueOf(frequencyMonths), MC).divide(BigDecimal.valueOf(12), MC);
    }

    static BigDecimal round(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP).setScale(4, RoundingMode.HALF_UP);
    }
}
