package com.sni.bokaticowork.features.inventory.asset.dto.request;

import com.sni.bokaticowork.features.inventory.asset.enums.AssetCondition;
import lombok.Data;

@Data
public class AssetReturnRequest {

    private AssetCondition returnCondition;

    private String returnedBy;

    private String returnPhotoUrl;

    private String notes;
}
