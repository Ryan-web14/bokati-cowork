package com.sni.bokaticowork.features.inventory.stock.dto.request;

import com.sni.bokaticowork.features.inventory.stock.enums.StockOutReasonCode;
import com.sni.bokaticowork.features.inventory.stock.enums.StockReferenceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class StockAdjustmentRequest {

    @NotBlank
    private String itemCode;

    @NotBlank
    private String locationCode;

    @NotNull
    private BigDecimal quantityDelta;

    private Long unitCost;

    private String lotNumber;

    private LocalDate expiryDate;

    private StockOutReasonCode reasonCode;

    private StockReferenceType referenceType;

    private String referenceCode;

    private String reasonDetails;

    private Boolean allowNegativeOverride;

    @NotBlank
    private String performedBy;
}
