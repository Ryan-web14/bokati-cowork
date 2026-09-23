package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;

/**
 * Ce qu'un client doit vraiment · et ce qui n'en est pas.
 *
 * <p>Un document de facturation porte un {@code balance_due} des sa creation. Ce montant ne
 * signifie « le client doit ceci » que pour certains documents, dans certains etats · le sommer
 * sans discernement additionnait des brouillons jamais emis et des avoirs, qui sont l'exact
 * contraire d'une dette.</p>
 *
 * <p>Deux erreurs constatees, opposees et toutes deux couteuses :</p>
 * <ul>
 *   <li>une facture en <b>brouillon</b> comptait comme due · le client voyait un solde qu'on ne
 *       lui avait jamais reclame, et une facture encore en preparation le faisait passer pour
 *       debiteur ;</li>
 *   <li>un <b>avoir</b> comptait comme du · on lui reclamait ce qu'on lui devait.</li>
 * </ul>
 *
 * <p>Les listes SQL des requetes d'agregation reprennent exactement ces ensembles. Quand l'une
 * change, l'autre change · c'est ecrit dans le commentaire de chaque requete concernee.</p>
 */
public final class BillingReceivables {

    /** Les types qui peuvent constituer une creance · un avoir n'en est pas un. */
    public static final Set<BillingDocumentType> RECEIVABLE_TYPES =
            EnumSet.of(BillingDocumentType.INVOICE, BillingDocumentType.PROFORMA_INVOICE);

    /**
     * Les etats ou rien n'est reclamable · avant emission, ou apres abandon.
     *
     * <p>{@code DRAFT} est le cas central : tant qu'une facture n'est pas emise, le client ne la
     * connait pas et ne peut rien devoir a son titre.</p>
     */
    public static final Set<BillingDocumentStatus> NOT_ISSUED = EnumSet.of(
            BillingDocumentStatus.DRAFT,
            BillingDocumentStatus.CANCELLED,
            BillingDocumentStatus.VOIDED,
            BillingDocumentStatus.REJECTED,
            BillingDocumentStatus.EXPIRED,
            BillingDocumentStatus.CONVERTED
    );

    /** Les etats ou la creance est eteinte · reglee, remboursee, ou abandonnee par nous. */
    public static final Set<BillingDocumentStatus> SETTLED = EnumSet.of(
            BillingDocumentStatus.PAID,
            BillingDocumentStatus.REFUNDED,
            BillingDocumentStatus.WRITTEN_OFF
    );

    private BillingReceivables() {
    }

    /** Le document existe pour le client · il lui a ete presente, et n'a pas ete annule. */
    public static boolean issued(BillingDocumentType type, BillingDocumentStatus status) {
        return RECEIVABLE_TYPES.contains(type) && status != null && !NOT_ISSUED.contains(status);
    }

    /** Le document constitue une creance en cours · c'est lui, et lui seul, qu'on peut reclamer. */
    public static boolean receivable(BillingDocumentType type, BillingDocumentStatus status) {
        return issued(type, status) && !SETTLED.contains(status);
    }

    /**
     * L'avoir est encore disponible · scelle, et pas encore consomme.
     *
     * <p>Sur un avoir, {@code ISSUED} signifie « consomme » : c'est l'etat pose au moment ou il est
     * impute sur une facture ou reverse au portefeuille. Un avoir encore utilisable est donc celui
     * qui est {@code VALIDATED}.</p>
     */
    public static boolean creditAvailable(BillingDocumentType type, BillingDocumentStatus status) {
        return type == BillingDocumentType.CREDIT_NOTE && status == BillingDocumentStatus.VALIDATED;
    }

    /**
     * Ce que ce document change au solde du client · positif s'il doit, negatif si on lui doit.
     *
     * <p>Un consommateur qui additionne cette valeur sur une liste de documents obtient le solde
     * juste, quels que soient les types et les etats presents. C'est ce qui manquait : chacun
     * sommait {@code balanceDue} et se trompait a sa maniere.</p>
     */
    public static BigDecimal customerImpact(BillingDocumentType type, BillingDocumentStatus status,
                                            BigDecimal balanceDue, BigDecimal totalAmount) {
        if (receivable(type, status)) {
            return balanceDue == null ? BigDecimal.ZERO : balanceDue;
        }
        if (creditAvailable(type, status)) {
            // Un avoir disponible reduit ce que le client doit · son montant compte en negatif.
            return totalAmount == null ? BigDecimal.ZERO : totalAmount.negate();
        }
        return BigDecimal.ZERO;
    }
}
