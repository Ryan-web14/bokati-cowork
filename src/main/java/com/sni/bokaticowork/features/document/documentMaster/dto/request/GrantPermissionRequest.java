package com.sni.bokaticowork.features.document.documentMaster.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GrantPermissionRequest {
    @NotBlank private String targetType;
    @NotBlank private String targetCode;
    @NotBlank private String granteeType;
    @NotNull private Long granteeId;
    @NotBlank private String permission;
}
