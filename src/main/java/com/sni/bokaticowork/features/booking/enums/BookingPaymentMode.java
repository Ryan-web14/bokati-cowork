package com.sni.bokaticowork.features.booking.enums;

/**
 * Comment une reservation est reglee.
 *
 * <p>Deux familles, et la distinction compte partout dans le module. Une reservation <b>payante</b>
 * produit un montant a regler et passe par le circuit de paiement. Une reservation <b>couverte par
 * un droit</b> consomme un abonnement ou un pass, ne coute rien de plus, et reserve puis libere ce
 * droit au fil de son cycle de vie.</p>
 *
 * <p>Le portefeuille appartient a la premiere famille : il ne donne aucun droit, il paie. Il s'en
 * distingue pourtant sur un point decisif. Les fonds sont saisis des la reservation, par un blocage
 * sur le solde du titulaire, ce qui rend la reservation aussi certaine qu'une reservation deja
 * reglee. Elle n'attend donc rien et se confirme immediatement ; seul le debit comptable est
 * differe, et il suit en arriere-plan. Le portefeuille a debiter est celui du titulaire, resolu
 * depuis le contexte de connexion, aucun identifiant n'etant attendu dans la demande.</p>
 */
public enum BookingPaymentMode {

    /** Regle par un moyen de paiement classique, especes, mobile money ou autre. */
    DIRECT(true, false),

    /** Regle depuis le portefeuille du titulaire, fonds bloques des la reservation. */
    WALLET(true, true),

    /** Couvert par un droit porte par un abonnement actif. */
    SUBSCRIPTION(false, false),

    /** Couvert par un droit porte par un pass. */
    PASS(false, false);

    private final boolean payable;
    private final boolean prepaid;

    BookingPaymentMode(boolean payable, boolean prepaid) {
        this.payable = payable;
        this.prepaid = prepaid;
    }

    /**
     * Vrai lorsque la reservation produit un montant a regler.
     */
    public boolean isPayable() {
        return payable;
    }

    /**
     * Vrai lorsque la reservation consomme un droit d'abonnement ou de pass, et doit donc le
     * reserver a la confirmation puis le liberer a l'annulation.
     */
    public boolean usesEntitlement() {
        return !payable;
    }

    /**
     * Vrai lorsque les fonds sont saisis a la reservation elle-meme. Le montant est alors acquis
     * avant toute confirmation, et le debit qui suit n'est plus qu'une ecriture.
     */
    public boolean isPrepaid() {
        return prepaid;
    }

    /**
     * Vrai lorsque la reservation doit attendre un reglement venu de l'exterieur avant d'etre
     * confirmee. C'est le seul cas qui justifie {@code PENDING_PAYMENT} : ni un droit, ni un
     * portefeuille deja bloque ne laissent la reservation en suspens.
     */
    public boolean awaitsPayment() {
        return payable && !prepaid;
    }
}
