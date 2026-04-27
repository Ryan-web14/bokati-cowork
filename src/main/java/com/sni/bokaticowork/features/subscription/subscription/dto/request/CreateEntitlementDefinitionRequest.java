package com.sni.bokaticowork.features.subscription.subscription.dto.request;

import com.sni.bokaticowork.features.subscription.subscription.enums.ConsumptionMode;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementResetPolicy;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementType;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateEntitlementDefinitionRequest(
        @NotBlank String name,
        String description,
        @NotNull EntitlementType entitlementType,
        @NotNull EntitlementUnit unit,
        @NotNull ConsumptionMode consumptionMode,
        @NotNull EntitlementResetPolicy resetPolicy,
        Boolean stackable,
        Boolean transferable,
        String resourceTypeCode,
        String resourceGroupCode,
        String metadataJson,
        Boolean active
) {
}
