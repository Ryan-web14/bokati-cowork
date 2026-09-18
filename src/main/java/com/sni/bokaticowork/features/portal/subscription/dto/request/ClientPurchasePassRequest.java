package com.sni.bokaticowork.features.portal.subscription.dto.request;

import com.sni.bokaticowork.features.subscription.subscription.enums.PassType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ClientPurchasePassRequest(
        @NotBlank String planCode,
        @NotNull PassType passType,
        @NotBlank String name,
        /** Cle facultative · un second envoi de la meme demande rend le pass deja achete. */
        String idempotencyKey
) {
}
