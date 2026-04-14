package com.sni.bokaticowork.features.inventory.stock.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class StockLevelResponse {

    private String itemCode;

    private String itemName;

    private String locationCode;

    private String locationName;

    private BigDecimal quantityOnHand;

    private BigDecimal quantityReserved;

    private BigDecimal quantityAvailable;

    private Long averageCost;

    private Instant lastMovementAt;
}
