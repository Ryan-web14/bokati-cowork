package com.sni.bokaticowork.features.billing.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Limites de remise fixees par configuration, par opposition a celles portees par chaque article
 * du catalogue.
 *
 * <p>Deux niveaux, qui repondent a des questions differentes :
 *
 * <ul>
 *   <li><b>Le repli par ligne</b> ({@code defaultMinNetRate}, {@code minLineNetAmount}) borne ce
 *       qu'une ligne peut consentir, meme lorsque le catalogue reste muet — ce qui est le cas de
 *       toute ligne libre, saisie sans article. C'est le garde-fou de dernier recours : par
 *       defaut, une ligne conserve au moins la moitie de son montant de base.</li>
 *   <li><b>Le seuil global</b> ({@code maxDocumentDiscountRate}, {@code maxDocumentDiscountAmount})
 *       borne l'offre dans son ensemble. Il porte sur la remise <b>totale</b>, remises de ligne
 *       comprises · ne compter que la remise document laisserait le contourner en eclatant la
 *       meme remise sur les lignes.</li>
 * </ul>
 *
 * <p>{@code enabled} ne gouverne que ces limites-ci. Celles du catalogue relevent de la politique
 * propre a chaque article ({@code discountPolicy}), et continuent de s'appliquer.
 */
@Data
@Component
@ConfigurationProperties(prefix = "bokati.billing.discount-guard")
public class BillingDiscountGuardProperties {

    /** A false, plus aucune limite de configuration. Celles du catalogue restent actives. */
    private boolean enabled = true;

    /**
     * Part minimale du montant de base qu'une ligne doit conserver apres remise, en pourcentage.
     * A 50, une ligne de 10 000 ne peut pas descendre sous 5 000. Vide : aucun repli.
     */
    private BigDecimal defaultMinNetRate = new BigDecimal("50");

    /** Montant net minimal absolu par ligne. Vide : seul le taux de repli s'applique. */
    private BigDecimal minLineNetAmount;

    /** Remise totale maximale, en pourcentage du sous-total. */
    private BigDecimal maxDocumentDiscountRate = new BigDecimal("50");

    /** Plafond de remise en valeur absolue. Vide : seul le taux s'applique. */
    private BigDecimal maxDocumentDiscountAmount;
}
