package com.sni.bokaticowork.features.inventory.admin.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventoryDashboardResponse {
    private long openAlerts;
    private long lowStockAlerts;
    private long outOfStockAlerts;
    private long negativeStockAlerts;
    private long expirySoonAlerts;
    private long expiringLots30Days;
    private long outOfStockLevels;
    private long openPurchaseOrders;
    private Long totalStockValue;
}
