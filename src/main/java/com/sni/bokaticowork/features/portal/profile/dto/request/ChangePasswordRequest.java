package com.sni.bokaticowork.features.portal.profile.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank
    private String currentPassword;

    @NotBlank
    @Size(min = 8, max = 45, message = "Password must be between 8 and 45 characters")
    private String newPassword;
}
