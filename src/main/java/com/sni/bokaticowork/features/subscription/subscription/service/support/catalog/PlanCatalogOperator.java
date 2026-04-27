package com.sni.bokaticowork.features.subscription.subscription.service.support.catalog;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceAlreadyExistException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.CreatePlanRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.CreatePlanVersionRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.UpdatePlanRequest;
import com.sni.bokaticowork.features.subscription.subscription.enums.PlanStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.model.SubscriptionPlan;
import com.sni.bokaticowork.features.subscription.repository.PlanBenefitRepository;
import com.sni.bokaticowork.features.subscription.repository.PlanEntitlementRepository;
import com.sni.bokaticowork.features.subscription.repository.PlanPriceRepository;
import com.sni.bokaticowork.features.subscription.repository.PlanVersionRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionPlanRepository;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionCodeFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PlanCatalogOperator {

    private final SubscriptionPlanRepository planRepository;
    private final PlanVersionRepository planVersionRepository;
    private final PlanPriceRepository planPriceRepository;
    private final PlanBenefitRepository planBenefitRepository;
    private final PlanEntitlementRepository planEntitlementRepository;
    private final SubscriptionCodeFactory codeFactory;
    private final PlanComponentBuilder componentBuilder;

    public SubscriptionPlan createPlan(CreatePlanRequest request) {
        String code = codeFactory.nextPlanCode(request);

        if (planRepository.existsByCodeIgnoreCase(code)) {
            throw new ResourceAlreadyExistException("A subscription plan with code " + code + " already exists");
        }

        return planRepository.save(SubscriptionPlan.builder()
                .code(code)
                .name(request.name().trim())
                .description(trim(request.description()))
                .planType(request.planType())
                .targetAudience(request.targetAudience())
                .status(PlanStatus.DRAFT)
                .visible(Boolean.TRUE.equals(request.visible()))
                .sortOrder(request.sortOrder())
                .build());
    }

    public SubscriptionPlan updatePlan(String planCode, UpdatePlanRequest request) {
        SubscriptionPlan plan = getPlanForService(planCode);
        if (StringUtils.hasText(request.name())) {
            plan.setName(request.name().trim());
        }
        if (request.description() != null) {
            plan.setDescription(trim(request.description()));
        }
        if (request.planType() != null) {
            plan.setPlanType(request.planType());
        }
        if (request.targetAudience() != null) {
            plan.setTargetAudience(request.targetAudience());
        }
        if (request.visible() != null) {
            plan.setVisible(request.visible());
        }
        if (request.sortOrder() != null) {
            plan.setSortOrder(request.sortOrder());
        }
        return planRepository.save(plan);
    }

    public void deletePlan(String planCode) {
        SubscriptionPlan plan = getPlanForService(planCode);
        List<PlanVersion> versions = planVersionRepository.findAllByPlanOrderByVersionNumberDesc(plan.getId());

        if (versions.isEmpty()) {
            planRepository.delete(plan);
            return;
        }

        plan.setStatus(PlanStatus.ARCHIVED);
        plan.setVisible(Boolean.FALSE);
        planRepository.save(plan);
    }

    public PlanVersion createVersion(String planCode, CreatePlanVersionRequest request) {
        SubscriptionPlan plan = getPlanForService(planCode);
        Integer nextVersion = planVersionRepository.maxVersionNumber(plan.getId()) + 1;

        PlanVersion version = planVersionRepository.save(PlanVersion.builder()
                .plan(plan)
                .versionNumber(nextVersion)
                .name(request.name().trim())
                .description(trim(request.description()))
                .status(PlanStatus.DRAFT)
                .effectiveFrom(request.effectiveFrom())
                .effectiveTo(request.effectiveTo())
                .termsJson(trim(request.termsJson()))
                .build());

        if (request.prices() != null) {
            request.prices().forEach(price -> planPriceRepository.save(componentBuilder.price(version, price)));
        }
        if (request.benefits() != null) {
            request.benefits().forEach(benefit -> planBenefitRepository.save(componentBuilder.benefit(version, benefit)));
        }
        if (request.entitlements() != null) {
            request.entitlements().forEach(entitlement -> planEntitlementRepository.save(componentBuilder.entitlement(version, entitlement)));
        }
        return version;
    }

    public PlanVersion publishVersion(String planCode, Long versionId) {
        SubscriptionPlan plan = getPlanForService(planCode);
        PlanVersion version = planVersionRepository.findById(versionId)
                .filter(candidate -> candidate.getPlan().getId().equals(plan.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Plan version not found"));

        planVersionRepository.findFirstByPlanAndStatusOrderByVersionNumberDesc(plan.getId(), PlanStatus.ACTIVE.name())
                .ifPresent(active -> {
                    active.setStatus(PlanStatus.ARCHIVED);
                    active.setEffectiveTo(LocalDate.now());
                    planVersionRepository.save(active);
                });

        version.setStatus(PlanStatus.ACTIVE);
        if (version.getEffectiveFrom() == null) {
            version.setEffectiveFrom(LocalDate.now());
        }
        plan.setStatus(PlanStatus.ACTIVE);
        plan.setVisible(Boolean.TRUE);
        planRepository.save(plan);
        return planVersionRepository.save(version);
    }

    public PlanVersion getVersionForService(String planCode, Long versionId) {
        SubscriptionPlan plan = getPlanForService(planCode);
        return planVersionRepository.findById(versionId)
                .filter(candidate -> candidate.getPlan().getId().equals(plan.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Plan version not found"));
    }

    public List<PlanVersion> listVersionsForService(String planCode) {
        SubscriptionPlan plan = getPlanForService(planCode);
        return planVersionRepository.findAllByPlanOrderByVersionNumberDesc(plan.getId());
    }

    public SubscriptionPlan getPlanForService(String planCode) {
        if (!StringUtils.hasText(planCode)) {
            throw new BadRequestException("Plan code is required");
        }
        return planRepository.findByCodeIgnoreCase(planCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan with code " + planCode + " not found"));
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
