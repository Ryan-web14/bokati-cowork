package com.sni.bokaticowork.features.inventory.asset.dto.request;

import com.sni.bokaticowork.features.inventory.asset.enums.AssetAssigneeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class AssetReserveRequest {

    @NotNull
    private AssetAssigneeType assigneeType;

    @NotBlank
    private String assigneeCode;

    private Instant reservedFrom;

    private Instant expectedReturnAt;

    private String reservedBy;

    private String notes;
}
