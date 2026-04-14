package com.sni.bokaticowork.features.inventory.stock.dto.response;

import com.sni.bokaticowork.features.inventory.stock.enums.StockMovementType;
import com.sni.bokaticowork.features.inventory.stock.enums.StockReferenceType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class StockMovementResponse {

    private String movementCode;

    private String itemCode;

    private String itemName;

    private String locationFromCode;

    private String locationToCode;

    private StockMovementType movementType;

    private BigDecimal quantity;

    private Long unitCost;

    private Long totalCost;

    private StockReferenceType referenceType;

    private String referenceCode;

    private String reason;

    private Boolean allowNegativeOverride;

    private Boolean reversed;

    private String reversalOfMovementCode;

    private String reversalMovementCode;

    private String originalMovementCode;

    private Instant reversedAt;

    private String reversedBy;

    private String reversalReason;

    private List<StockMovementLotResponse> lots;

    private String performedBy;

    private Instant performedAt;
}
