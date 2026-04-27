package com.sni.bokaticowork.features.subscription.subscription.mapper.interfaces;

import com.sni.bokaticowork.features.subscription.subscription.dto.response.EntitlementDefinitionResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.PlanBenefitResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.PlanEntitlementResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.PlanPriceResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.PlanResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.PlanVersionResponse;
import com.sni.bokaticowork.features.subscription.subscription.mapper.decorator.SubscriptionCatalogMapperDecorator;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementDefinition;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanBenefit;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanEntitlement;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanPrice;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.model.SubscriptionPlan;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(SubscriptionCatalogMapperDecorator.class)
public interface SubscriptionCatalogMapper {

    default PlanResponse toPlanResponse(SubscriptionPlan plan) {
        return null;
    }

    default PlanResponse toPlanResponse(SubscriptionPlan plan, boolean includeVersions) {
        return null;
    }

    default PlanVersionResponse toPlanVersionResponse(PlanVersion version) {
        return null;
    }

    default PlanPriceResponse toPlanPriceResponse(PlanPrice price) {
        return null;
    }

    default PlanBenefitResponse toPlanBenefitResponse(PlanBenefit benefit) {
        return null;
    }

    default PlanEntitlementResponse toPlanEntitlementResponse(PlanEntitlement entitlement) {
        return null;
    }

    default EntitlementDefinitionResponse toEntitlementDefinitionResponse(EntitlementDefinition definition) {
        return null;
    }
}
