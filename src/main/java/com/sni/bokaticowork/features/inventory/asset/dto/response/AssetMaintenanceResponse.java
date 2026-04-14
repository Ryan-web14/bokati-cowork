package com.sni.bokaticowork.features.inventory.asset.dto.response;

import com.sni.bokaticowork.features.inventory.asset.enums.AssetMaintenanceStatus;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetMaintenanceType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AssetMaintenanceResponse {

    private String maintenanceCode;
    private String assetCode;
    private AssetMaintenanceType maintenanceType;
    private AssetMaintenanceStatus status;
    private Instant scheduledAt;
    private Instant startedAt;
    private Instant completedAt;
    private String providerName;
    private Long cost;
    private String description;
    private String resolution;
}
