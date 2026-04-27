package com.sni.bokaticowork.features.subscription.subscription.service.support.catalog;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.PlanBenefitRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.PlanEntitlementRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.PlanPriceRequest;
import com.sni.bokaticowork.features.subscription.subscription.enums.BenefitCategory;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementDefinition;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanBenefit;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanEntitlement;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanPrice;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import com.sni.bokaticowork.features.subscription.repository.EntitlementDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class PlanComponentBuilder {

    private final EntitlementDefinitionRepository entitlementDefinitionRepository;

    public PlanPrice price(PlanVersion version, PlanPriceRequest request) {
        return PlanPrice.builder()
                .planVersion(version)
                .billingCycle(request.billingCycle())
                .currency(request.currency().trim().toUpperCase(Locale.ROOT))
                .amount(request.amount())
                .setupFee(request.setupFee() == null ? BigDecimal.ZERO : request.setupFee())
                .depositAmount(request.depositAmount() == null ? BigDecimal.ZERO : request.depositAmount())
                .taxIncluded(request.taxIncluded() == null || request.taxIncluded())
                .commitmentMonths(request.commitmentMonths() == null ? 0 : request.commitmentMonths())
                .build();
    }

    public PlanBenefit benefit(PlanVersion version, PlanBenefitRequest request) {
        return PlanBenefit.builder()
                .planVersion(version)
                .title(request.title().trim())
                .description(trim(request.description()))
                .icon(trim(request.icon()))
                .category(request.category() == null ? BenefitCategory.CUSTOM : request.category())
                .displayOrder(request.displayOrder())
                .highlighted(Boolean.TRUE.equals(request.highlighted()))
                .included(request.included() == null || request.included())
                .metadataJson(trim(request.metadataJson()))
                .build();
    }

    public PlanEntitlement entitlement(PlanVersion version, PlanEntitlementRequest request) {
        EntitlementDefinition definition = entitlementDefinitionRepository.findByCodeIgnoreCase(request.entitlementCode().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Entitlement definition " + request.entitlementCode() + " not found"));
        boolean unlimited = Boolean.TRUE.equals(request.unlimited());
        if (!unlimited && request.quantity() == null) {
            throw new BadRequestException("Entitlement quantity is required when unlimited is false");
        }

        return PlanEntitlement.builder()
                .planVersion(version)
                .entitlementDefinition(definition)
                .quantity(unlimited ? null : request.quantity())
                .unlimited(unlimited)
                .rolloverAllowed(Boolean.TRUE.equals(request.rolloverAllowed()))
                .rolloverLimit(request.rolloverLimit())
                .validForDays(request.validForDays())
                .priority(request.priority() == null ? 100 : request.priority())
                .restrictionsJson(trim(request.restrictionsJson()))
                .build();
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
