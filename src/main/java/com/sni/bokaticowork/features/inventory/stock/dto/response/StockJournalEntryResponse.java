package com.sni.bokaticowork.features.inventory.stock.dto.response;

import com.sni.bokaticowork.features.inventory.stock.enums.StockJournalDirection;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class StockJournalEntryResponse {

    private String movementCode;

    private String itemCode;

    private String locationCode;

    private String stockAccount;

    private String counterpartAccount;

    private StockJournalDirection direction;

    private Long amount;

    private LocalDate accountingDate;

    private String periodCode;

    private String label;
}
