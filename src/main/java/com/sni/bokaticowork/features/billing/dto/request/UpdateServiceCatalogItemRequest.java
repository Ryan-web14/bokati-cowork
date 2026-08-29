package com.sni.bokaticowork.features.billing.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Modification d'un article de catalogue · seuls les champs presents sont appliques.
 *
 * <p>Consequence a connaitre : un champ absent est conserve, il ne peut donc pas etre efface par
 * cet appel. Pour retirer un plancher ou une limite de remise, passer par
 * {@code clearFields} en y nommant les champs a remettre a vide.
 *
 * @param clearFields noms des champs a remettre a null, par exemple {@code ["floorPrice"]}
 */
public record UpdateServiceCatalogItemRequest(
        String name,
        String description,
        String category,
        String unit,
        BigDecimal unitPrice,
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
        LocalDate validUntil,

        List<String> clearFields
) {
}
