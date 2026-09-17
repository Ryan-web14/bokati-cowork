package com.sni.bokaticowork.features.inventory.catalog.dto.response;

import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryPriceType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class InventoryItemPriceHistoryResponse {

    private Long id;

    private String itemCode;

    private InventoryPriceType priceType;

    private Long previousValue;

    private Long newValue;

    /** newValue moins previousValue, null lorsque l'un des deux est absent. */
    private Long delta;

    private String changedBy;

    private String reason;

    private Instant changedAt;
}
