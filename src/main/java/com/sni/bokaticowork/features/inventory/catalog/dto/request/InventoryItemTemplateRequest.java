package com.sni.bokaticowork.features.inventory.catalog.dto.request;

import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryItemType;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryTrackingType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class InventoryItemTemplateRequest {

    @NotBlank
    private String name;

    private String description;

    private String categoryCode;

    private String unitCode;

    @NotNull
    private InventoryItemType itemType;

    private InventoryTrackingType trackingType;

    private Long defaultCost;

    private Long salePrice;

    private Boolean active;

    @NotEmpty
    @Valid
    private List<Axis> axes;

    @Data
    public static class Axis {

        @NotBlank
        private String axisCode;

        @NotBlank
        private String name;

        private Integer position;

        @NotEmpty
        @Valid
        private List<Value> values;
    }

    @Data
    public static class Value {

        @NotBlank
        private String valueCode;

        @NotBlank
        private String label;

        private Integer position;

        private Boolean active;
    }
}
