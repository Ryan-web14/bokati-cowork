package com.sni.bokaticowork.features.inventory.control.dto.response;

import com.sni.bokaticowork.features.inventory.control.enums.InventoryCountStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class InventoryCountResponse {
    private String countCode;
    private String locationCode;
    private String locationName;
    private InventoryCountStatus status;
    private String createdBy;
    private String notes;
    private Instant startedAt;
    private Instant reviewedAt;
    private Instant validatedAt;
    private Instant createdAt;
    private List<InventoryCountItemResponse> items;
}
