package com.sni.bokaticowork.features.inventory.asset.dto.response;

import com.sni.bokaticowork.features.inventory.asset.enums.AssetAssigneeType;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetCondition;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class AssetResponse {

    private String assetCode;
    private String itemCode;
    private String itemName;
    private String serialNumber;
    private String assetTag;
    private AssetStatus status;
    private AssetCondition condition;
    private String locationCode;
    private String locationName;
    private AssetAssigneeType assignedToType;
    private String assignedToCode;
    private LocalDate purchaseDate;
    private Long purchaseCost;
    private LocalDate warrantyEndDate;
    private Integer usefulLifeMonths;
    private Long residualValue;
    private Long depreciatedValue;
    private String notes;
}
