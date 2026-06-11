package com.sni.bokaticowork.features.inventory.asset.dto.request;

import com.sni.bokaticowork.features.inventory.asset.enums.AssetAssigneeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class AssetAssignRequest {

    @NotNull
    private AssetAssigneeType assigneeType;

    @NotBlank
    private String assigneeCode;

    private Instant startAt;

    @NotNull
    private Instant expectedReturnAt;

    @NotBlank
    private String assignedBy;

    @NotBlank
    private String purpose;

    private String checkoutPhotoUrl;

    private String receiverSignatureUrl;

    private String notes;
}
