package com.sni.bokaticowork.features.portal.profile.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateAvatarRequest {

    @NotBlank
    @Size(max = 500)
    private String avatarUrl;
}
