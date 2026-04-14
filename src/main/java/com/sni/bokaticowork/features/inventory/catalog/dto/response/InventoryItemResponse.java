package com.sni.bokaticowork.features.inventory.catalog.dto.response;

import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryItemType;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryTrackingType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventoryItemResponse {

    private String itemCode;

    private String name;

    private String description;

    private String psku;

    private String shortCode;

    private String displayCode;

    private String identificationCode;

    private String specification;

    private String categoryCode;

    private String categoryName;

    private String unitCode;

    private String unitName;

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
