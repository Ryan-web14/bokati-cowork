package com.sni.bokaticowork.features.portal.profile.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangeEmailConfirmRequest {

    @NotBlank
    private String ottToken;

    @NotBlank
    private String verificationToken;
}
