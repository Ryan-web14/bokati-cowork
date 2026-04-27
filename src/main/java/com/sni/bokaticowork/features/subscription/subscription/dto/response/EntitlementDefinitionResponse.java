package com.sni.bokaticowork.features.subscription.subscription.dto.response;

import com.sni.bokaticowork.features.subscription.subscription.enums.ConsumptionMode;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementResetPolicy;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementType;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementUnit;

public record EntitlementDefinitionResponse(
        Long id,
        String code,
        String name,
        String description,
        EntitlementType entitlementType,
        EntitlementUnit unit,
        ConsumptionMode consumptionMode,
        EntitlementResetPolicy resetPolicy,
        Boolean stackable,
        Boolean transferable,
        String resourceTypeCode,
        String resourceGroupCode,
        String metadataJson,
        Boolean active
) {
}
