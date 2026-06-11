package com.sni.bokaticowork.features.inventory.intelligence.dto.response;

import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryItemType;
import com.sni.bokaticowork.features.inventory.intelligence.enums.ConsumptionTrend;
import com.sni.bokaticowork.features.inventory.intelligence.enums.ReorderSuggestionReason;
import com.sni.bokaticowork.features.inventory.intelligence.enums.ReorderSuggestionSeverity;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class ReorderSuggestionResponse {
    private String itemCode;
    private String itemName;
    private InventoryItemType itemType;
    private String categoryCode;
    private String categoryName;
    private String unitCode;
    private String unitName;
    private String locationCode;
    private String locationName;
    private String ruleScope;
    private BigDecimal currentQuantity;
    private BigDecimal quantityOnHand;
    private BigDecimal quantityReserved;
    private BigDecimal minQuantity;
    private BigDecimal maxQuantity;
    private BigDecimal reorderQuantity;
    private BigDecimal targetQuantity;
    private BigDecimal shortageQuantity;
    private Long estimatedUnitCost;
    private Long estimatedOrderCost;
    private String preferredSupplierCode;
    private ReorderSuggestionSeverity severity;
    private ReorderSuggestionReason reasonCode;
    private String reason;
    private Integer priorityScore;
    private BigDecimal consumptionRateLast30Days;
    private BigDecimal consumptionRatePrev30Days;
    private ConsumptionTrend consumptionTrend;
    private Integer consumptionTrendPercent;
    private Integer daysOfStockRemaining;
    private BigDecimal quantityOnOrder;
    private Instant recommendedOrderByDate;
    private String bestSupplierCode;
    private Long bestSupplierPrice;
    private Integer bestSupplierLeadTimeDays;
}
