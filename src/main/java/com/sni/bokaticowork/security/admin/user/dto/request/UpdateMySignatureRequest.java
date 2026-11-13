package com.sni.bokaticowork.security.admin.user.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Signature de l'utilisateur connecte, en data URI PNG ou JPEG (lot B12). */
public record UpdateMySignatureRequest(@NotBlank String signatureImageBase64) {
}
