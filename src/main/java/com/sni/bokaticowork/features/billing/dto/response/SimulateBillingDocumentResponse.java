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
        List<SimulatedLine> lines,
        BigDecimal subtotalAmount,
        BigDecimal lineDiscountAmount,
        BigDecimal documentDiscountAmount,
        BigDecimal discountAmount,
        BigDecimal taxableAmount,
        BigDecimal vatAmount,
        BigDecimal additionalCentAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount) {

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
