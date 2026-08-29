package com.sni.bokaticowork.features.billing.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Creation d'un article de catalogue.
 *
 * <p>Tous les champs ajoutes apres {@code displayOrder} sont facultatifs. Laisses vides, l'article
 * se comporte exactement comme avant l'enrichissement : aucun plancher, aucune limite de remise,
 * aucune contrainte de saisie.
 *
 * @param minMarginRate    marge minimale en %, rapportee au prix de vente (taux de marque). Sert a
 *                         deriver le plancher lorsque {@code floorPrice} n'est pas saisi.
 * @param discountPolicy   {@code NONE}, {@code WARN} ou {@code BLOCK}. Defaut {@code NONE}.
 * @param includedItems    ce que la prestation comprend · rendu en sous-lignes sur le document.
 */
public record CreateServiceCatalogItemRequest(
        @NotBlank String name,
        String description,
        String category,
        String unit,
        @NotNull BigDecimal unitPrice,
        String currency,
        String taxRuleCode,
        Integer displayOrder,

        @PositiveOrZero BigDecimal costPrice,
        @PositiveOrZero BigDecimal floorPrice,
        @DecimalMin("0") @DecimalMax(value = "100", inclusive = false) BigDecimal minMarginRate,
        @DecimalMin("0") @DecimalMax("100") BigDecimal maxDiscountRate,
        String discountPolicy,

        String detailedDescription,
        List<String> includedItems,
        String imageUrl,

        String billingMode,
        @PositiveOrZero BigDecimal defaultQuantity,
        @PositiveOrZero BigDecimal minQuantity,
        @PositiveOrZero BigDecimal maxQuantity,
        Boolean taxableByDefault,

        String subcategory,
        List<String> tags,
        String externalReference,
        LocalDate validFrom,
        LocalDate validUntil
) {
}
