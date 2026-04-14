package com.sni.bokaticowork.features.inventory.stock.dto.request;

import com.sni.bokaticowork.features.inventory.stock.enums.StockReferenceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class StockReservationRequest {
    @NotBlank
    private String itemCode;
    @NotBlank
    private String locationCode;
    @NotNull
    @Positive
    private BigDecimal quantity;
    private StockReferenceType referenceType;
    private String referenceCode;
    private String reservedBy;
    private Instant expiresAt;
}
