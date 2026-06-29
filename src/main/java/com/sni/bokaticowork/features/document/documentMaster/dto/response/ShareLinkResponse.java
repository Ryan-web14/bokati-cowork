package com.sni.bokaticowork.features.document.documentMaster.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ShareLinkResponse {
    private String token;
    private String shareUrl;
    private String documentCode;
    private String folderCode;
    private Boolean allowDownload;
    private Boolean passwordProtected;
    private Integer maxAccessCount;
    private Integer accessCount;
    private Instant expiresAt;
    private Instant createdAt;
    private Long createdBy;
    private String createdByEmail;
    private Boolean active;
}
