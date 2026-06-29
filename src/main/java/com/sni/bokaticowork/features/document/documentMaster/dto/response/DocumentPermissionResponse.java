package com.sni.bokaticowork.features.document.documentMaster.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class DocumentPermissionResponse {
    private Long id;
    private String targetType;
    private Long targetId;
    private String targetCode;
    private String granteeType;
    private Long granteeId;
    private String granteeEmail;
    private String permission;
    private Long grantedBy;
    private String grantedByEmail;
    private Instant grantedAt;
}
