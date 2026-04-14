package com.sni.bokaticowork.features.inventory.asset.dto.request;

import com.sni.bokaticowork.features.inventory.asset.enums.AssetMaintenanceType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class AssetMaintenanceRequest {

    @NotNull
    private AssetMaintenanceType maintenanceType;

    private Instant scheduledAt;

    private String providerName;

    private Long cost;

    private String description;
}
