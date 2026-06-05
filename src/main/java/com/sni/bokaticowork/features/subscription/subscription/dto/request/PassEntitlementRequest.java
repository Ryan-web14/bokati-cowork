package com.sni.bokaticowork.features.subscription.subscription.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sni.bokaticowork.core.configuration.FrontendInstantDeserializer;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.Instant;

public record PassEntitlementRequest(
        @NotBlank String entitlementCode,
        BigDecimal quantity,
        Boolean unlimited,
        @JsonDeserialize(using = FrontendInstantDeserializer.class) Instant validFrom,
        @JsonDeserialize(using = FrontendInstantDeserializer.class) Instant validUntil
) {
}
