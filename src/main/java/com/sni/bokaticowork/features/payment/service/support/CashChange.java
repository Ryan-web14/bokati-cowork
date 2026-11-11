package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Monnaie a rendre au guichet.
 *
 * <p>Regle unique, partagee par l'apercu et l'encaissement :</p>
 * <ul>
 *   <li>montant encaisse = {@code amount} s'il est donne, sinon le plus petit du montant remis et du
 *   reste a payer (un client qui tend moins que le du fait un paiement partiel) ;</li>
 *   <li>monnaie rendue = montant remis moins montant encaisse ;</li>
 *   <li>un montant remis inferieur a un {@code amount} explicite est refuse.</li>
 * </ul>
 *
 * @param appliedAmount  ce qui est impute sur la dette et entre en caisse
 * @param tenderedAmount ce que le client a tendu, nul si non saisi
 * @param changeAmount   la monnaie a rendre, nulle si rien n'a ete saisi comme remis
 */
public record CashChange(BigDecimal appliedAmount, BigDecimal tenderedAmount, BigDecimal changeAmount) {

    public static CashChange compute(BigDecimal payable, BigDecimal amount, BigDecimal tendered) {
        BigDecimal due = money(payable == null ? BigDecimal.ZERO : payable.max(BigDecimal.ZERO));
        if (tendered != null && tendered.signum() <= 0) {
            throw new BadRequestException("Le montant remis par le client doit être positif");
        }
        BigDecimal requested = amount == null || amount.signum() <= 0 ? null : money(amount);
        if (tendered == null) {
            return new CashChange(requested == null ? due : requested, null, null);
        }
        BigDecimal given = money(tendered);
        BigDecimal applied = requested != null ? requested : given.min(due);
        if (given.compareTo(applied) < 0) {
            throw new BadRequestException("Le montant remis (" + given.stripTrailingZeros().toPlainString()
                    + ") est inférieur au montant à encaisser (" + applied.stripTrailingZeros().toPlainString() + ")");
        }
        return new CashChange(applied, given, given.subtract(applied));
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(4, RoundingMode.HALF_UP);
    }
}
