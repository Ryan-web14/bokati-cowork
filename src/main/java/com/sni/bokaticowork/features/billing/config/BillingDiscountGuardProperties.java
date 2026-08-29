package com.sni.bokaticowork.features.billing.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Seuil global de remise, applique au document entier.
 *
 * <p>Complete les limites par article du catalogue : celles-ci encadrent chaque prestation,
 * celui-ci encadre l'offre dans son ensemble. Le seuil porte sur la remise <b>totale</b>, remises
 * de ligne comprises — ne compter que la remise document laisserait le contourner en eclatant la
 * meme remise sur les lignes.
 */
@Data
@Component
@ConfigurationProperties(prefix = "bokati.billing.discount-guard")
public class BillingDiscountGuardProperties {

    /** A false, aucun controle global · les limites par article continuent de s'appliquer. */
    private boolean enabled = true;

    /** Remise totale maximale, en pourcentage du sous-total. */
    private BigDecimal maxDocumentDiscountRate = new BigDecimal("20");

    /** Plafond en valeur absolue. Vide, seul le taux s'applique. */
    private BigDecimal maxDocumentDiscountAmount;
}
