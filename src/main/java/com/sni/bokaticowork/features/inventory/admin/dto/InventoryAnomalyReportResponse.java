package com.sni.bokaticowork.features.inventory.admin.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventoryAnomalyReportResponse {
    private long negativeStockLevels;
    private long negativeStockAlerts;
    private long movementsWithNegativeOverride;
    private long suspiciousAdjustments;
    private long largeInventoryCountVariances;
}
