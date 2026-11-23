package com.sni.bokaticowork.features.payment.service.support;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Lot K1 (A14, revue comptable) : ce qu'un remboursement reprend sur la transaction d'origine.
 *
 * <p>La transaction est vue comme une suite de tranches : d'abord sa part non affectee (trop-percu,
 * somme en attente au 4719), puis ses affectations dans l'ordre. Les remboursements successifs
 * consomment cette suite l'un apres l'autre : le premier prend le debut, le suivant reprend ou le
 * precedent s'est arrete. Une affectation n'est donc jamais rouverte deux fois, et un trop-percu
 * rembourse ne rouvre aucune facture.</p>
 *
 * <p>Regle commune a la facturation (factures rouvertes) et a la comptabilite (D 411 par facture
 * rouverte, D 4719 pour la part non affectee).</p>
 */
public final class RefundSplit {

    private RefundSplit() {
    }

    /** Affectation d'une transaction a une facture, ou part de remboursement qui rouvre cette facture. */
    public record Part(String documentNumber, BigDecimal amount) {
    }

    /** Part non affectee remboursee et affectations rouvertes. */
    public record Result(BigDecimal unallocated, List<Part> reopened) {
    }

    /**
     * @param paid            montant de la transaction d'origine
     * @param allocations     ses affectations, dans l'ordre
     * @param alreadyRefunded montant deja rembourse par les remboursements aboutis precedents
     * @param amount          montant de ce remboursement
     */
    public static Result split(BigDecimal paid, List<Part> allocations, BigDecimal alreadyRefunded, BigDecimal amount) {
        BigDecimal allocated = allocations.stream().map(Part::amount).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal free = nz(paid).subtract(allocated).max(BigDecimal.ZERO);
        BigDecimal start = nz(alreadyRefunded).max(BigDecimal.ZERO);
        BigDecimal end = start.add(nz(amount).max(BigDecimal.ZERO));
        BigDecimal cursor = free;
        BigDecimal unallocated = overlap(BigDecimal.ZERO, free, start, end);
        List<Part> reopened = new ArrayList<>();
        BigDecimal taken = unallocated;
        for (Part allocation : allocations) {
            BigDecimal size = nz(allocation.amount()).max(BigDecimal.ZERO);
            BigDecimal part = overlap(cursor, cursor.add(size), start, end);
            if (part.signum() > 0) {
                reopened.add(new Part(allocation.documentNumber(), part));
                taken = taken.add(part);
            }
            cursor = cursor.add(size);
        }
        // Au-dela de la transaction (ne devrait pas arriver, le remboursement est plafonne) : en attente.
        BigDecimal rest = nz(amount).subtract(taken);
        if (rest.signum() > 0) {
            unallocated = unallocated.add(rest);
        }
        return new Result(unallocated, List.copyOf(reopened));
    }

    private static BigDecimal overlap(BigDecimal from, BigDecimal to, BigDecimal start, BigDecimal end) {
        BigDecimal low = from.max(start);
        BigDecimal high = to.min(end);
        return high.compareTo(low) > 0 ? high.subtract(low) : BigDecimal.ZERO;
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
