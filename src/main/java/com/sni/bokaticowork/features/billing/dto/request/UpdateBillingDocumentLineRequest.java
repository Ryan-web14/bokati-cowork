package com.sni.bokaticowork.features.billing.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.sni.bokaticowork.features.billing.enums.BillingLineType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateBillingDocumentLineRequest(
        /** UPSERT crée ou met à jour la ligne, REMOVE la supprime. */
        @NotNull String action,
        @NotNull Integer lineOrder,
        BillingLineType lineType,
        /**
         * Code de l'article du catalogue. {@code catalogSourceCode} est accepte comme alias :
         * c'est le nom qu'emploie l'interface, et un ecart de nom sur ce champ n'echoue pas — il
         * desarme silencieusement le rattachement au catalogue, donc le plancher de prix, le taux
         * de remise maximal et la reprise de la categorie et de l'unite.
         */
        @JsonAlias("catalogSourceCode") String itemCode,
        @NotBlank String description,
        String detailedDescription,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal discountRate,
        BigDecimal discountAmount,
        Boolean taxable,
        Boolean taxIncluded,
        BigDecimal vatRate,
        BigDecimal additionalCentRate,
        String unit,
        /** {@code catalogSourceType} accepte comme alias · voir {@link CreateBillingDocumentLineRequest}. */
        @JsonAlias("catalogSourceType") String sourceType,
        String sourceCode,
        String externalReference,
        String notes,
        Boolean optional,
        /** Voir {@link CreateBillingDocumentLineRequest#category()}. */
        String category
) {
}
