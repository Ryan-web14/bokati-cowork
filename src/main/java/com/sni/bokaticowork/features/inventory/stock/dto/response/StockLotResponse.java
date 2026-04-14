package com.sni.bokaticowork.features.inventory.stock.dto.response;

import lombok.Builder;
import lombok.Data;
import com.sni.bokaticowork.features.inventory.stock.enums.StockOwnershipType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class StockLotResponse {
    private String itemCode;
    private String itemName;
    private String locationCode;
    private String locationName;
    private String lotNumber;
    private LocalDate expiryDate;
    private Instant receivedAt;
    private BigDecimal initialQuantity;
    private BigDecimal remainingQuantity;
    private Boolean quarantined;
    private String quarantineReason;
    private StockOwnershipType ownershipType;
    private String ownerCode;
    private Boolean active;
}
