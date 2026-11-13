package com.sni.bokaticowork.security.admin.user.dto.response;

import java.time.Instant;

/**
 * Fiche collaborateur (lot B12).
 *
 * @param signatureImageBase64 la signature enregistree, pour l'apercu ; vide s'il n'y en a pas
 */
public record StaffProfileResponse(
        String userCode,
        String email,
        String fullName,
        String jobTitle,
        boolean hasSignature,
        String signatureImageBase64,
        Instant signatureUpdatedAt,
        String managerUserCode,
        String managerName,
        String managerEmail
) {
}
