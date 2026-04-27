package com.sni.bokaticowork.features.subscription.subscription.mapper.decorator;

import com.sni.bokaticowork.features.subscription.repository.PlanBenefitRepository;
import com.sni.bokaticowork.features.subscription.repository.PlanEntitlementRepository;
import com.sni.bokaticowork.features.subscription.repository.PlanPriceRepository;
import com.sni.bokaticowork.features.subscription.repository.PlanVersionRepository;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.EntitlementDefinitionResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.PlanBenefitResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.PlanEntitlementResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.PlanPriceResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.PlanResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.PlanVersionResponse;
import com.sni.bokaticowork.features.subscription.subscription.enums.PlanStatus;
import com.sni.bokaticowork.features.subscription.subscription.mapper.interfaces.SubscriptionCatalogMapper;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementDefinition;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanBenefit;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanEntitlement;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanPrice;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.model.SubscriptionPlan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public abstract class SubscriptionCatalogMapperDecorator implements SubscriptionCatalogMapper {

    @Autowired
    @Qualifier("delegate")
    private SubscriptionCatalogMapper delegate;

    @Autowired
    private PlanVersionRepository planVersionRepository;

    @Autowired
    private PlanPriceRepository planPriceRepository;

    @Autowired
    private PlanBenefitRepository planBenefitRepository;

    @Autowired
    private PlanEntitlementRepository planEntitlementRepository;

    @Override
    public PlanResponse toPlanResponse(SubscriptionPlan plan) {
        return toPlanResponse(plan, true);
    }

    @Override
    public PlanResponse toPlanResponse(SubscriptionPlan plan, boolean includeVersions) {
        PlanVersionResponse activeVersion = planVersionRepository.findFirstByPlanAndStatusOrderByVersionNumberDesc(
                plan.getId(),
                PlanStatus.ACTIVE.name()
        ).map(this::toPlanVersionResponse).orElse(null);

        List<PlanVersionResponse> versions = includeVersions
                ? planVersionRepository.findAllByPlanOrderByVersionNumberDesc(plan.getId()).stream().map(this::toPlanVersionResponse).toList()
                : List.of();

        return new PlanResponse(
                plan.getId(),
                plan.getCode(),
                plan.getName(),
                plan.getDescription(),
                plan.getPlanType(),
                plan.getTargetAudience(),
                plan.getStatus(),
                plan.getVisible(),
                plan.getSortOrder(),
                plan.getCreatedAt(),
                plan.getUpdatedAt(),
                activeVersion,
                versions
        );
    }

    @Override
    public PlanVersionResponse toPlanVersionResponse(PlanVersion version) {
        return new PlanVersionResponse(
                version.getId() == null ? null : String.valueOf(version.getId()),
                version.getVersionNumber(),
                version.getName(),
                version.getDescription(),
                version.getStatus(),
                version.getEffectiveFrom(),
                version.getEffectiveTo(),
                version.getTermsJson(),
                planPriceRepository.findAllByPlanVersion(version.getId()).stream().map(this::toPlanPriceResponse).toList(),
                planBenefitRepository.findAllByPlanVersionOrderByDisplayOrderAscCreatedAtAsc(version.getId()).stream().map(this::toPlanBenefitResponse).toList(),
                planEntitlementRepository.findAllByPlanVersionOrderByPriorityAsc(version.getId()).stream().map(this::toPlanEntitlementResponse).toList()
        );
    }

    @Override
    public PlanPriceResponse toPlanPriceResponse(PlanPrice price) {
        return new PlanPriceResponse(price.getId(), price.getBillingCycle(), price.getCurrency(), price.getAmount(), price.getSetupFee(), price.getDepositAmount(), price.getTaxIncluded(), price.getTrialDays(), price.getCommitmentMonths(), price.getTaxCode());
    }

    @Override
    public PlanBenefitResponse toPlanBenefitResponse(PlanBenefit benefit) {
        return new PlanBenefitResponse(benefit.getId(), benefit.getTitle(), benefit.getDescription(), benefit.getIcon(), benefit.getCategory(), benefit.getDisplayOrder(), benefit.getHighlighted(), benefit.getIncluded(), benefit.getMetadataJson());
    }

    @Override
    public PlanEntitlementResponse toPlanEntitlementResponse(PlanEntitlement entitlement) {
        return new PlanEntitlementResponse(entitlement.getId(), entitlement.getEntitlementDefinition().getCode(), entitlement.getEntitlementDefinition().getName(), entitlement.getQuantity(), entitlement.getUnlimited(), entitlement.getRolloverAllowed(), entitlement.getRolloverLimit(), entitlement.getValidForDays(), entitlement.getPriority(), entitlement.getRestrictionsJson());
    }

    @Override
    public EntitlementDefinitionResponse toEntitlementDefinitionResponse(EntitlementDefinition definition) {
        return new EntitlementDefinitionResponse(definition.getId(), definition.getCode(), definition.getName(), definition.getDescription(), definition.getEntitlementType(), definition.getUnit(), definition.getConsumptionMode(), definition.getResetPolicy(), definition.getStackable(), definition.getTransferable(), definition.getResourceType() == null ? null : definition.getResourceType().getCode(), definition.getResourceGroup() == null ? null : definition.getResourceGroup().getCode(), definition.getMetadataJson(), definition.getActive());
    }
}
