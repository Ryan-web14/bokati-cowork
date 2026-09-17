package com.sni.bokaticowork.features.inventory.catalog.dto.response;

import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryPackagingLevel;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class InventoryPackagingResponse {

    private Long id;

    private String itemCode;

    private InventoryPackagingLevel packagingLevel;

    private String name;

    private BigDecimal quantity;

    private String barcodeValue;

    private BigDecimal weightKg;

    private Integer lengthMm;

    private Integer widthMm;

    private Integer heightMm;

    private Boolean active;
}
