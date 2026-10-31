package com.sni.bokaticowork.features.subscription.usage.dto;

import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementUnit;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateUsageRecordRequest(
        @NotNull SubscriberType ownerType,
        @NotBlank String ownerCode,
        @NotBlank String entitlementCode,
        @NotNull BigDecimal quantity,
        @NotNull EntitlementUnit unit,
        @NotBlank String referenceType,
        @NotBlank String referenceId,
        Boolean consumeEntitlement,
        Boolean billable,
        BigDecimal billableAmount,
        String currency,
        Instant occurredAt,
        String metadataJson,
        /** Qui a consomme quand ce n'est pas le titulaire · vide sinon. */
        String beneficiaryType,
        String beneficiaryCode
) {

    public CreateUsageRecordRequest(SubscriberType ownerType, String ownerCode, String entitlementCode, BigDecimal quantity,
                                    EntitlementUnit unit, String referenceType, String referenceId, Boolean consumeEntitlement,
                                    Boolean billable, BigDecimal billableAmount, String currency, Instant occurredAt,
                                    String metadataJson) {
        this(ownerType, ownerCode, entitlementCode, quantity, unit, referenceType, referenceId, consumeEntitlement,
                billable, billableAmount, currency, occurredAt, metadataJson, null, null);
    }
}
