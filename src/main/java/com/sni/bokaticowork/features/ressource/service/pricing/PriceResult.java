package com.sni.bokaticowork.features.ressource.service.pricing;

import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lot R1 : prix calcule par le moteur unique, avec sa trace.
 *
 * @param unit         unite retenue (la moins chere pour le client, ou celle imposee)
 * @param unitPrice    prix moyen d'une unite (montant / unites / multiplicateur)
 * @param units        nombre d'unites (heures fractionnees, ou forfaits)
 * @param multiplier   quantite facturee (postes, ou participants pour une regle par participant)
 * @param amount       montant total
 * @param lines        tranches facturees (heures pleines et creuses, jours)
 * @param alternatives cout de chaque unite possible, pour l'explication
 */
public record PriceResult(ResourceBookingUnit unit, BigDecimal unitPrice, BigDecimal units, int multiplier, BigDecimal amount,
                          String currency, List<Line> lines, List<Alternative> alternatives) {

    /** Une tranche : debut, fin (texte ISO), unites, prix d'une unite, regles appliquees, montant. */
    public record Line(String from, String to, BigDecimal units, BigDecimal unitPrice, Long baseRuleId, String baseRuleLabel,
                       List<Adjustment> adjustments, BigDecimal amount) {
    }

    /** Ajustement applique a une tranche. */
    public record Adjustment(Long ruleId, String label, String type, Integer value) {
    }

    /** Cout d'une unite candidate (pour comprendre le choix). */
    public record Alternative(ResourceBookingUnit unit, BigDecimal amount, boolean chosen) {
    }

    public static PriceResult free(ResourceBookingUnit unit, int multiplier) {
        return new PriceResult(unit, BigDecimal.ZERO, BigDecimal.ZERO, multiplier, BigDecimal.ZERO.setScale(4), "XAF", List.of(), List.of());
    }
}
