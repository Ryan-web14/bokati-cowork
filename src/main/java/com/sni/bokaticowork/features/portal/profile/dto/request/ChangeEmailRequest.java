package com.sni.bokaticowork.features.portal.profile.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangeEmailRequest {

    @Email
    @NotBlank
    @Size(max = 250)
    private String newEmail;

    @NotBlank
    private String currentPassword;
}
