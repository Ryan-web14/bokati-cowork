package com.sni.bokaticowork.features.inventory.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InventoryItemSubstituteRequest {

    @NotBlank
    private String substituteItemCode;

    /** Ordre de proposition, le plus petit en premier. */
    private Integer priority;

    /** Quantite de substitut equivalente a une unite de l'article d'origine. */
    @Positive
    private BigDecimal conversionFactor;

    /** Vrai pour que le substitut propose en retour l'article d'origine. */
    private Boolean bidirectional;

    private String notes;

    private Boolean active;
}
