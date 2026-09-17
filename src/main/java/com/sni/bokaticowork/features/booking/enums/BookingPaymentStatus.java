package com.sni.bokaticowork.features.booking.enums;

import java.time.Instant;

/**
 * Ou en est le reglement d'une reservation, du point de vue de celui qui l'a prise.
 *
 * <p>La question n'avait aucune reponse jusqu'ici. Le client lisait un statut de reservation et
 * devait en deduire seul ce qu'il devait encore. C'est tenable tant que le statut suffit, et ca
 * cesse de l'etre avec le portefeuille : les fonds sont saisis a la confirmation, le debit suit en
 * arriere-plan, et rien dans {@link BookingStatus} ne distingue cette reservation deja payee d'une
 * reservation qui attend encore un reglement.</p>
 *
 * <p>Le libelle est rendu par le serveur. L'interface affiche ce qu'elle recoit, elle ne traduit
 * pas un code et n'a aucune regle metier a rejouer.</p>
 */
public enum BookingPaymentStatus {

    /** Couvert par un abonnement ou un pass. Rien n'est du. */
    COVERED("Couvert"),

    /** Un reglement est attendu. */
    PENDING("En attente de paiement"),

    /** Les fonds sont acquis. */
    PAID("Payé"),

    /** Reservation annulee ou refusee. Plus rien n'est attendu. */
    CANCELLED("Annulé");

    private final String label;

    BookingPaymentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Deduit l'etat de reglement des seuls faits que porte la reservation, sans aucune lecture
     * annexe : la reponse doit couter la meme chose sur une liste de deux cents reservations que
     * sur une seule.
     *
     * <p>L'equivalence entre confirmation et reglement n'est pas une approximation. Une reservation
     * au portefeuille pose son blocage a l'interieur de la transaction de confirmation : si le
     * solde ne suffit pas, la confirmation n'a pas lieu, donc confirmee signifie fonds acquis. Une
     * reservation reglee de l'exterieur n'est confirmee que par la chaine de paiement, et seulement
     * une fois l'intention integralement soldee.</p>
     */
    public static BookingPaymentStatus of(BookingPaymentMode mode, BookingStatus status, Instant confirmedAt) {
        if (status == BookingStatus.CANCELLED || status == BookingStatus.REJECTED) {
            return CANCELLED;
        }
        // Un mode absent n'est pas un mode couvert : dans le doute, on annonce un reglement
        // attendu plutot qu'un droit dont rien ne prouve l'existence.
        if (mode != null && mode.usesEntitlement()) {
            return COVERED;
        }
        return confirmedAt == null ? PENDING : PAID;
    }
}
