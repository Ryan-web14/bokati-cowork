package com.sni.bokaticowork.features.inventory.catalog.dto.response;

import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryItemType;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryTrackingType;
import com.sni.bokaticowork.features.inventory.catalog.enums.ItemLifecycleStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

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

    private ItemLifecycleStatus lifecycleStatus;

    /** Derive du statut de cycle de vie, conserve pour compatibilite. */
    private Boolean active;

    private String revision;

    private BigDecimal weightKg;

    private BigDecimal volumeM3;

    private Integer lengthMm;

    private Integer widthMm;

    private Integer heightMm;

    private Boolean stackable;

    /** Renseigne lorsque cet article est une variante generee depuis un modele. */
    private String templateCode;

    private String variantSignature;
}
