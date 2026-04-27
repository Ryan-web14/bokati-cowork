package com.sni.bokaticowork.features.inventory.catalog.dto.request;

import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryItemType;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryTrackingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InventoryItemRequest {

    @NotBlank
    private String name;

    private String description;

    private String psku;

    private String identificationCode;

    private String specification;

    private String categoryCode;

    private String unitCode;

    @NotNull
    private InventoryItemType itemType;

    private InventoryTrackingType trackingType;

    private Long defaultCost;

    private Long salePrice;

    private Boolean taxable;

    private Boolean allowNegativeStock;

    private Boolean requiresExpiryDate;

    private Boolean requiresLotNumber;

    private Boolean requiresSerialNumber;

    private Boolean active;
}
