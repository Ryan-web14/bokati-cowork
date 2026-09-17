package com.sni.bokaticowork.features.inventory.catalog.dto.response;

import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryItemType;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryTrackingType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class InventoryItemTemplateResponse {

    private String templateCode;

    private String name;

    private String description;

    private String categoryCode;

    private String categoryName;

    private String unitCode;

    private InventoryItemType itemType;

    private InventoryTrackingType trackingType;

    private Long defaultCost;

    private Long salePrice;

    private Boolean active;

    /** Nombre de combinaisons possibles a partir des valeurs actives. */
    private int possibleCombinations;

    /** Nombre de variantes deja creees. */
    private long generatedVariants;

    private List<Axis> axes;

    @Data
    @Builder
    public static class Axis {
        private String axisCode;
        private String name;
        private Integer position;
        private List<Value> values;
    }

    @Data
    @Builder
    public static class Value {
        private String valueCode;
        private String label;
        private Integer position;
        private Boolean active;
    }
}
