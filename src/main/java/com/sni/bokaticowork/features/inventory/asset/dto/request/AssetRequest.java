package com.sni.bokaticowork.features.inventory.asset.dto.request;

import com.sni.bokaticowork.features.inventory.asset.enums.AssetCondition;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AssetRequest {

    private String assetCode;

    @NotBlank
    private String itemCode;

    private String serialNumber;

    private String assetTag;

    private AssetStatus status;

    private AssetCondition condition;

    private String locationCode;

    private LocalDate purchaseDate;

    private Long purchaseCost;

    private LocalDate warrantyEndDate;

    private Integer usefulLifeMonths;

    private Long residualValue;

    private String notes;
}
