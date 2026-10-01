package com.sni.bokaticowork.features.payment.cash.dto.request;

import jakarta.validation.constraints.Size;

/** Pourquoi l annonce est abandonnee · le client s est ravise, ou la caisse fait le menage. */
public record CloseCashDeclarationRequest(
        @Size(max = 500) String reason
) {
}
