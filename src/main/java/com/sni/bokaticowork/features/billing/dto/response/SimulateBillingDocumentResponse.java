package com.sni.bokaticowork.features.billing.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Totaux d'un document tel qu'il serait calcule, sans qu'aucun document ne soit cree.
 *
 * <p>Repond au besoin de connaitre un montant avant de s'engager : jusqu'ici le seul moyen
 * d'obtenir un total etait de creer le document, ce qui consommait un numero de sequence et
 * laissait des brouillons derriere chaque essai de remise.
 *
 * <p>Le detail par ligne est la partie utile : il donne, ligne a ligne, le brut, la remise
 * appliquee et le net, ce qui remplace le calcul manuel a la calculatrice.
 *
 * @param lineDiscountAmount     remises portees par les lignes elles-memes
 * @param documentDiscountAmount remise appliquee au document entier
 * @param discountAmount         somme des deux · c'est ce montant que le seuil global controle
 */
public record SimulateBillingDocumentResponse(
        /**
         * Type, devise et titre retenus · repris de la requete ou completes par defaut. L'apercu
         * peut ainsi afficher son en-tete sans avoir a deviner ce que le serveur a suppose.
         */
        com.sni.bokaticowork.features.billing.enums.BillingDocumentType documentType,
        String currency,
        String title,
        List<SimulatedLine> lines,
        BigDecimal subtotalAmount,
        BigDecimal lineDiscountAmount,
        BigDecimal documentDiscountAmount,
        BigDecimal discountAmount,
        BigDecimal taxableAmount,
        BigDecimal vatAmount,
        BigDecimal additionalCentAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        /**
         * Depassements constates · la simulation les annonce sans jamais refuser. Ceux de
         * severite {@code BLOCK} empecheront l'emission, sauf derogation motivee. C'est ce qui
         * permet a l'interface d'avertir pendant la saisie plutot qu'au moment d'emettre.
         */
        List<com.sni.bokaticowork.features.billing.service.support.BillingDiscountGuard.DiscountViolation> violations,
        Boolean blocked,
        /**
         * Lot B13 : toutes les alertes, ligne par ligne quand elles en concernent une ({@code lineIndex},
         * 0 pour la premiere) : prix plancher et remise maximale, stock, organisation bloquee,
         * plafond d'encours, exoneration appliquee. {@code severity} BLOCK : l'emission sera refusee.
         */
        List<Alert> alerts) {

    /** Alerte de simulation ; {@code lineIndex} vide pour une alerte du document. */
    public record Alert(Integer lineIndex, String code, String severity, String message) {}

    /**
     * @param netAmount      brut moins remise · le montant reellement facture pour cette ligne
     * @param effectiveRate  taux de remise reellement obtenu, meme lorsque la remise a ete saisie
     *                       en montant · c'est le chiffre que l'on cherche a la calculatrice
     */
    public record SimulatedLine(
            String itemCode,
            String description,
            String category,
            String unit,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal subtotalAmount,
            BigDecimal discountRate,
            BigDecimal discountAmount,
            BigDecimal effectiveRate,
            BigDecimal netAmount,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            Boolean taxable,
            Boolean optional) {}
}
