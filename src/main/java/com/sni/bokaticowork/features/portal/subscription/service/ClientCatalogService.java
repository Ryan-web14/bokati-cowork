package com.sni.bokaticowork.features.portal.subscription.service;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.portal.subscription.dto.request.ClientPurchasePassRequest;
import com.sni.bokaticowork.features.portal.subscription.dto.request.ClientSubscribeRequest;
import com.sni.bokaticowork.features.portal.subscription.dto.response.ClientPlanDetailResponse;
import com.sni.bokaticowork.features.portal.subscription.dto.response.ClientPlanSummaryResponse;
import com.sni.bokaticowork.features.subscription.repository.specification.criteria.PlanSearchCriteria;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.CreatePassRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.CreateSubscriptionRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.PassResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.PlanBenefitResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.PlanPriceResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.PlanResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.PlanVersionResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.SubscriptionResponse;
import com.sni.bokaticowork.features.subscription.subscription.enums.PlanStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.PlanType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.PassService;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.SubscriptionCatalogService;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientCatalogService {

    private final SubscriptionCatalogService catalogService;
    private final SubscriptionService subscriptionService;
    private final PassService passService;

    @Transactional(readOnly = true)
    public PaginatedResponse<ClientPlanSummaryResponse> listAvailablePlans(PlanType planType, Pageable pageable) {
        PlanSearchCriteria criteria = PlanSearchCriteria.builder()
                .status(PlanStatus.ACTIVE)
                .visible(true)
                .planType(planType)
                .build();

        PaginatedResponse<PlanResponse> plans = catalogService.listPlans(criteria, pageable);

        PaginatedResponse<ClientPlanSummaryResponse> result = new PaginatedResponse<>();
        result.setData(plans.getData().stream().map(this::toSummary).toList());
        result.setPageable(plans.getPageable());
        return result;
    }

    @Transactional(readOnly = true)
    public ClientPlanDetailResponse getPlanDetail(String planCode) {
        PlanResponse plan = catalogService.getPlan(planCode);
        if (!Boolean.TRUE.equals(plan.visible()) || plan.status() != PlanStatus.ACTIVE) {
            throw new ResourceNotFoundException("Plan not found");
        }
        return toDetail(plan);
    }

    @Transactional
    public SubscriptionResponse subscribe(Member member, ClientSubscribeRequest request) {
        CreateSubscriptionRequest createRequest = new CreateSubscriptionRequest(
                request.planCode(),
                null,
                request.billingCycle(),
                SubscriberType.MEMBER,
                member.getMemberId(),
                LocalDate.now(),
                request.autoRenew() != null ? request.autoRenew() : true,
                null,
                false
        );
        return subscriptionService.create(createRequest);
    }

    @Transactional
    public PassResponse purchasePass(Member member, ClientPurchasePassRequest request) {
        PlanResponse plan = catalogService.getPlan(request.planCode());
        if (!Boolean.TRUE.equals(plan.visible()) || plan.status() != PlanStatus.ACTIVE) {
            throw new ResourceNotFoundException("Plan not found");
        }
        PlanVersionResponse activeVersion = plan.activeVersion();
        if (activeVersion == null) {
            throw new ResourceNotFoundException("No active version available for this plan");
        }

        CreatePassRequest createRequest = new CreatePassRequest(
                request.passType(),
                SubscriberType.MEMBER,
                member.getMemberId(),
                null,
                Long.valueOf(activeVersion.id()),
                request.name(),
                plan.description(),
                Instant.now().toString(),
                null,
                false,
                false,
                null,
                null,
                request.idempotencyKey(),
                null
        );
        return passService.create(createRequest);
    }

    private ClientPlanSummaryResponse toSummary(PlanResponse plan) {
        PlanVersionResponse version = plan.activeVersion();
        BigDecimal startingPrice = null;
        String currency = null;
        List<ClientPlanSummaryResponse.HighlightedBenefit> highlights = List.of();

        if (version != null) {
            if (version.prices() != null && !version.prices().isEmpty()) {
                PlanPriceResponse cheapest = version.prices().stream()
                        .min(Comparator.comparing(PlanPriceResponse::amount))
                        .orElse(null);
                if (cheapest != null) {
                    startingPrice = cheapest.amount();
                    currency = cheapest.currency();
                }
            }
            if (version.benefits() != null) {
                highlights = version.benefits().stream()
                        .filter(b -> Boolean.TRUE.equals(b.highlighted()))
                        .map(b -> ClientPlanSummaryResponse.HighlightedBenefit.builder()
                                .title(b.title())
                                .description(b.description())
                                .icon(b.icon())
                                .build())
                        .toList();
            }
        }

        return ClientPlanSummaryResponse.builder()
                .code(plan.code())
                .name(plan.name())
                .description(plan.description())
                .planType(plan.planType())
                .targetAudience(plan.targetAudience())
                .sortOrder(plan.sortOrder())
                .startingPrice(startingPrice)
                .currency(currency)
                .highlightedBenefits(highlights)
                .build();
    }

    private ClientPlanDetailResponse toDetail(PlanResponse plan) {
        PlanVersionResponse version = plan.activeVersion();

        return ClientPlanDetailResponse.builder()
                .code(plan.code())
                .name(plan.name())
                .description(plan.description())
                .planType(plan.planType())
                .targetAudience(plan.targetAudience())
                .sortOrder(plan.sortOrder())
                .requiredKycLevel(null)
                .termsJson(version != null ? version.termsJson() : null)
                .prices(version != null ? version.prices() : List.of())
                .benefits(version != null ? version.benefits() : List.of())
                .entitlements(version != null ? version.entitlements() : List.of())
                .build();
    }
}
