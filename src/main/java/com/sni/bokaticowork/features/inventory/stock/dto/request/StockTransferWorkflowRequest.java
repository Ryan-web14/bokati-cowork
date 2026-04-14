package com.sni.bokaticowork.features.inventory.stock.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockTransferWorkflowRequest {
    @NotBlank
    private String itemCode;

    @NotBlank
    private String fromLocationCode;

    @NotBlank
    private String toLocationCode;

    @NotNull
    @Positive
    private BigDecimal quantity;

    private String requestedBy;

    private String reason;
}
