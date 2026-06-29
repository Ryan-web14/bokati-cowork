package com.sni.bokaticowork.features.document.documentMaster.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateShareLinkRequest {
    @NotNull private Integer expiresInHours;
    private String password;
    private Boolean allowDownload;
    private Integer maxAccessCount;
}
