package com.sni.bokaticowork.features.inventory.asset.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AssetLocationHistoryResponse {
    private String assetCode;
    private String fromLocationCode;
    private String toLocationCode;
    private String changedBy;
    private String reason;
    private Instant changedAt;
}
