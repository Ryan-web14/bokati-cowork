package com.sni.bokaticowork.features.inventory.catalog.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class InventoryItemSubstituteResponse {

    private Long id;

    private String itemCode;

    private String substituteItemCode;

    private String substituteItemName;

    private Integer priority;

    private BigDecimal conversionFactor;

    private Boolean bidirectional;

    /** Vrai lorsque le lien est herite d'une declaration reciproque portee par l'autre article. */
    private Boolean inherited;

    private String notes;

    private Boolean active;
}
