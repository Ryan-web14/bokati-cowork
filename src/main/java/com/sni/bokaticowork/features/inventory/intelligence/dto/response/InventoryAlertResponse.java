package com.sni.bokaticowork.features.inventory.intelligence.dto.response;

import com.sni.bokaticowork.features.inventory.intelligence.enums.InventoryAlertStatus;
import com.sni.bokaticowork.features.inventory.intelligence.enums.InventoryAlertType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class InventoryAlertResponse {
    private String alertCode;
    private InventoryAlertType alertType;
    private InventoryAlertStatus status;
    private String itemCode;
    private String itemName;
    private String locationCode;
    private String locationName;
    private String assetCode;
    private BigDecimal currentQuantity;
    private BigDecimal thresholdQuantity;
    private String message;
    private Instant createdAt;
    private Instant acknowledgedAt;
    private Instant resolvedAt;
}
