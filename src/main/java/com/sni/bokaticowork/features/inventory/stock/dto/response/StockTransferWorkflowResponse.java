package com.sni.bokaticowork.features.inventory.stock.dto.response;

import com.sni.bokaticowork.features.inventory.stock.enums.StockTransferWorkflowStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class StockTransferWorkflowResponse {
    private String transferCode;
    private String itemCode;
    private String itemName;
    private String fromLocationCode;
    private String toLocationCode;
    private BigDecimal quantity;
    private StockTransferWorkflowStatus status;
    private String requestedBy;
    private String approvedBy;
    private String shippedBy;
    private String receivedBy;
    private String movementCode;
    private String reason;
    private Instant requestedAt;
    private Instant approvedAt;
    private Instant shippedAt;
    private Instant receivedAt;
}
