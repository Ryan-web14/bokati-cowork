package com.sni.bokaticowork.features.inventory.catalog.dto.request;

import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryItemType;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryTrackingType;
import com.sni.bokaticowork.features.inventory.catalog.enums.ItemLifecycleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

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

    /**
     * Statut de cycle de vie. Prioritaire sur le booleen active lorsque les deux sont fournis.
     */
    private ItemLifecycleStatus lifecycleStatus;

    /** Conserve pour compatibilite. Traduit en statut lorsque lifecycleStatus est absent. */
    private Boolean active;

    private String revision;

    private BigDecimal weightKg;

    private BigDecimal volumeM3;

    private Integer lengthMm;

    private Integer widthMm;

    private Integer heightMm;

    private Boolean stackable;
}
