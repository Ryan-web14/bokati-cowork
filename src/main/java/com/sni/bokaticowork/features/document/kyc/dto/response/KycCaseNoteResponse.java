package com.sni.bokaticowork.features.document.kyc.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class KycCaseNoteResponse {
    private Long id;
    private String kycCaseCode;
    private String content;
    private Long authorId;
    private Instant createdAt;
    private Boolean internal;
}
