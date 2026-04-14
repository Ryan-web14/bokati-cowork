package com.sni.bokaticowork.features.inventory.asset.dto.request;

import com.sni.bokaticowork.features.inventory.asset.enums.AssetCondition;
import lombok.Data;

@Data
public class AssetMaintenanceCompleteRequest {

    private String resolution;

    private Long cost;

    private AssetCondition assetCondition;
}
