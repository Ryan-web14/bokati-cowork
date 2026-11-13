package com.sni.bokaticowork.security.admin.user.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Mise a jour de la fiche collaborateur par un administrateur. Un champ absent reste inchange.
 *
 * @param managerUserCode code utilisateur du superieur direct ; chaine vide pour le retirer
 * @param signatureImageBase64 data URI PNG ou JPEG ; {@code clearSignature} pour l'effacer
 */
public record UpdateStaffProfileRequest(
        @Size(max = 160) String jobTitle,
        String managerUserCode,
        String signatureImageBase64,
        Boolean clearSignature
) {
}
