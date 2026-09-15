package com.sni.bokaticowork.features.inventory.catalog.dto.request;

import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryPackagingLevel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InventoryPackagingRequest {

    @NotNull
    private InventoryPackagingLevel packagingLevel;

    private String name;

    @NotNull
    @Positive
    private BigDecimal quantity;

    private String barcodeValue;

    private BigDecimal weightKg;

    private Integer lengthMm;

    private Integer widthMm;

    private Integer heightMm;

    private Boolean active;
}
