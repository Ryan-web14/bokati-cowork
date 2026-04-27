package com.sni.bokaticowork.features.subscription.addon.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateSubscriptionAddonRequest(
        @NotNull Long planVersionId,
        @Min(1) Integer quantity,
        LocalDate startsAt,
        LocalDate endsAt,
        String metadataJson
) {
}
