package com.sni.bokaticowork.features.inventory.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InventoryUnitConversionRequest {
    @NotBlank
    private String itemCode;

    @NotBlank
    private String fromUnitCode;

    @NotBlank
    private String toUnitCode;

    @NotNull
    @Positive
    private BigDecimal factor;
}
