package com.sni.bokaticowork.features.inventory.catalog.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class InventoryItemRevisionHistoryResponse {

    private Long id;

    private String itemCode;

    private String previousRevision;

    private String newRevision;

    private String changedBy;

    private String reason;

    private String documentCode;

    private Instant changedAt;
}
