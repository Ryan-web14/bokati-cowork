package com.sni.bokaticowork.features.document.documentMaster.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class DocumentAccessLogResponse {
    private Long id;
    private String documentCode;
    private Long userId;
    private String action;
    private Instant accessedAt;
    private String ipAddress;
    private String userAgent;
}
