package com.sni.bokaticowork.features.inventory.asset.dto.response;

import com.sni.bokaticowork.features.inventory.asset.enums.AssetAssignmentStatus;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetAssigneeType;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetCondition;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AssetAssignmentResponse {

    private String assetCode;
    private AssetAssigneeType assigneeType;
    private String assigneeCode;
    private AssetAssignmentStatus status;
    private Instant startAt;
    private Instant expectedReturnAt;
    private Instant endAt;
    private String assignedBy;
    private String purpose;
    private String returnedBy;
    private AssetCondition checkoutCondition;
    private AssetCondition returnCondition;
    private String checkoutPhotoUrl;
    private String returnPhotoUrl;
    private String receiverSignatureUrl;
    private String notes;
}
