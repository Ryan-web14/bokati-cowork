package com.sni.bokaticowork.features.document.kyc.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class KycTimelineEntryResponse {
    private Instant timestamp;
    private String action;
    private String actor;
    private String description;
}
