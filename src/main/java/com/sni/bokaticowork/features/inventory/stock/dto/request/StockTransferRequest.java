package com.sni.bokaticowork.features.inventory.stock.dto.request;

import com.sni.bokaticowork.features.inventory.stock.enums.StockReferenceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockTransferRequest {

    @NotBlank
    private String itemCode;

    @NotBlank
    private String fromLocationCode;

    @NotBlank
    private String toLocationCode;

    @NotNull
    @Positive
    private BigDecimal quantity;

    private StockReferenceType referenceType;

    private String referenceCode;

    private String reason;

    private Boolean allowNegativeOverride;

    private String performedBy;
}
