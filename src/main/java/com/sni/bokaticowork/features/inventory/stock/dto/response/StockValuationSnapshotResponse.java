package com.sni.bokaticowork.features.inventory.stock.dto.response;

import com.sni.bokaticowork.features.inventory.stock.enums.ValuationMethod;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class StockValuationSnapshotResponse {

    private LocalDate snapshotDate;

    private String periodCode;

    private String itemCode;

    private String locationCode;

    private BigDecimal quantityOnHand;

    private Long unitCost;

    private Long totalValue;

    private ValuationMethod valuationMethod;

    private Integer oldestLayerAgeDays;
}
