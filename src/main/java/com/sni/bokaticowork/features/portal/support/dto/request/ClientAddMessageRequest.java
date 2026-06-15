package com.sni.bokaticowork.features.portal.support.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClientAddMessageRequest(
        @NotBlank @Size(max = 10000) String content
) {}
