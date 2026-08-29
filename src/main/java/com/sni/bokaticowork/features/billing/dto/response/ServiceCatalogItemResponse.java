package com.sni.bokaticowork.features.billing.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Article de catalogue.
 *
 * @param effectiveFloorPrice plancher reellement applique, qu'il ait ete saisi ou derive du cout
 *                            et de la marge minimale. C'est cette valeur que l'interface doit
 *                            afficher, et non {@code floorPrice} qui peut etre vide alors qu'un
 *                            plancher existe bel et bien.
 * @param floorPriceOrigin    d'ou vient ce plancher · {@code EXPLICIT}, {@code DERIVED_FROM_MARGIN},
 *                            {@code COST_PRICE} ou {@code NONE}. Permet d'expliquer le blocage a
 *                            l'utilisateur plutot que de lui opposer un nombre sans justification.
 * @param currentlyValid      l'article est-il dans sa fenetre de validite aujourd'hui
 */
public record ServiceCatalogItemResponse(
        String itemCode,
        String name,
        String description,
        String category,
        String unit,
        BigDecimal unitPrice,
        String currency,
        String taxRuleCode,
        Boolean active,
        Integer displayOrder,
        Instant createdAt,

        BigDecimal costPrice,
        BigDecimal floorPrice,
        BigDecimal minMarginRate,
        BigDecimal maxDiscountRate,
        String discountPolicy,
        BigDecimal effectiveFloorPrice,
        String floorPriceOrigin,

        String detailedDescription,
        List<String> includedItems,
        String imageUrl,

        String billingMode,
        BigDecimal defaultQuantity,
        BigDecimal minQuantity,
        BigDecimal maxQuantity,
        Boolean taxableByDefault,

        String subcategory,
        List<String> tags,
        String externalReference,
        LocalDate validFrom,
        LocalDate validUntil,
        Boolean currentlyValid
) {
}
