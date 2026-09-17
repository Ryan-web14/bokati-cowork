package com.sni.bokaticowork.features.inventory.stock.dto.response;

import com.sni.bokaticowork.features.inventory.stock.enums.InventoryPeriodStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class InventoryPeriodResponse {

    private String periodCode;

    private LocalDate startDate;

    private LocalDate endDate;

    private InventoryPeriodStatus status;

    private String closedBy;

    private Instant closedAt;

    private String reopenedBy;

    private Instant reopenedAt;

    private String reopenReason;

    private Long closingStockValue;
}
