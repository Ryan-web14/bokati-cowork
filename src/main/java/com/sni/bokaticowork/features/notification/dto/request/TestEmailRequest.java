package com.sni.bokaticowork.features.notification.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record TestEmailRequest(
        @Email @NotBlank String to,
        String subject,
        String content,
        Boolean html
) {
}
