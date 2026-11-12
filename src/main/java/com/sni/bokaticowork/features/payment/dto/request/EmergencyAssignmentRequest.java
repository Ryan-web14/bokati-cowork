package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Affectation d'urgence par un superviseur : la personne peut tenir la caisse pendant quelques
 * heures (12 au plus par defaut), sans attendre la mise a jour des affectations.
 */
public record EmergencyAssignmentRequest(
        @NotBlank String userEmail,
        Integer hours,
        @NotBlank String reason
) {
}
