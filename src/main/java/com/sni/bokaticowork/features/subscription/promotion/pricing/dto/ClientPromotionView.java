package com.sni.bokaticowork.features.subscription.promotion.pricing.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Ce qu'un client voit de ses avantages.
 *
 * <p>La liste montre aussi ce a quoi il n'a <b>pas</b> droit, avec la raison. C'est un choix
 * delibere : un client qui comprend pourquoi son code est refuse n'ecrit pas au support, et celui
 * qui voit ce qui lui manque pour en beneficier a une raison d'y revenir.</p>
 *
 * @param code                nul pour une promotion automatique · il n'y a rien a saisir
 * @param estimatedSaving     economie sur le panier courant, ou nulle si elle n'est pas calculable
 * @param ineligibilityReason redige en francais, jamais un code technique
 */
public record ClientPromotionView(
        Kind kind,
        String code,
        String label,
        String description,
        BigDecimal estimatedSaving,
        String currency,
        Instant validUntil,
        Status status,
        String ineligibilityReason
) {

    public enum Kind {

        /** S'applique d'elle-meme · le client n'a rien a faire. */
        AUTOMATIC,

        /** Reservee a ce client, nommement. */
        NOMINATIVE,

        /** Un code qui lui a ete attribue. */
        COUPON_ASSIGNED,

        /** Un code public, valable pour qui le connait. */
        COUPON_PUBLIC
    }

    public enum Status {

        /** Utilisable maintenant. */
        APPLICABLE,

        /** Deja pris en compte dans le prix affiche. */
        ALREADY_APPLIED,

        /** Existe, mais ce client ou ce panier n'y ouvre pas droit. */
        NOT_ELIGIBLE,

        EXPIRED,

        /** Plus de solde, ou budget de campagne consomme. */
        EXHAUSTED
    }

    public static ClientPromotionView applicable(Kind kind, String code, String label, String description,
                                                 BigDecimal saving, String currency, Instant validUntil) {
        return new ClientPromotionView(kind, code, label, description, saving, currency, validUntil,
                Status.APPLICABLE, null);
    }

    public static ClientPromotionView refused(Kind kind, String code, String label, String description,
                                              Instant validUntil, Status status, String reason) {
        return new ClientPromotionView(kind, code, label, description, null, null, validUntil, status, reason);
    }
}
