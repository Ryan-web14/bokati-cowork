package com.sni.bokaticowork.features.subscription.repository.specification.criteria;

import com.sni.bokaticowork.features.subscription.subscription.enums.ConsumptionMode;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementResetPolicy;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementType;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class EntitlementDefinitionSearchCriteria {
    private String query;
    private String code;
    private String name;
    private EntitlementType entitlementType;
    private EntitlementUnit unit;
    private ConsumptionMode consumptionMode;
    private EntitlementResetPolicy resetPolicy;
    private String resourceTypeCode;
    private String resourceGroupCode;
    private Boolean stackable;
    private Boolean transferable;
    private Boolean active;
}
