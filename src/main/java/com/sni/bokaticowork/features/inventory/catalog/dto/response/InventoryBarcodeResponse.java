package com.sni.bokaticowork.features.inventory.catalog.dto.response;

import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryBarcodeType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class InventoryBarcodeResponse {

    private Long id;

    private String itemCode;

    private InventoryBarcodeType barcodeType;

    private String barcodeValue;

    private String unitCode;

    private BigDecimal quantity;

    private Boolean primaryCode;

    private Boolean active;

    private Instant createdAt;
}
