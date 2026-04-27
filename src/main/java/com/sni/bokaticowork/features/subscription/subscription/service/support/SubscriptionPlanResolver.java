package com.sni.bokaticowork.features.subscription.subscription.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import com.sni.bokaticowork.features.subscription.subscription.enums.PlanStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanPrice;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.model.SubscriptionPlan;
import com.sni.bokaticowork.features.subscription.repository.PlanPriceRepository;
import com.sni.bokaticowork.features.subscription.repository.PlanVersionRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class SubscriptionPlanResolver {

    private static final long MAX_SAFE_JAVASCRIPT_INTEGER = 9_007_199_254_740_991L;

    private final PlanVersionRepository planVersionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final PlanPriceRepository planPriceRepository;

    public PlanVersion resolvePlanVersion(String planCode, String planVersionId) {
        if (StringUtils.hasText(planVersionId)) {
            String rawPlanVersionId = planVersionId.trim();
            Long parsedPlanVersionId = parsePlanVersionId(rawPlanVersionId);
            PlanVersion version = planVersionRepository.findById(parsedPlanVersionId).orElse(null);
            if (version != null && version.getStatus() == PlanStatus.ACTIVE) {
                return version;
            }

            if (StringUtils.hasText(planCode)) {
                return resolveLatestActiveVersionByPlanCode(planCode);
            }

            if (version != null) {
                return resolveLatestActiveVersionByPlan(version.getPlan(),
                        "Plan version " + rawPlanVersionId + " is not active and no active fallback version was found");
            }

            throw new ResourceNotFoundException(missingPlanVersionMessage(rawPlanVersionId, parsedPlanVersionId));
        }

        return resolveLatestActiveVersionByPlanCode(planCode);
    }

    public PlanPrice resolvePrice(PlanVersion planVersion, BillingCycle requestedCycle) {
        if (requestedCycle != null) {
            return planPriceRepository.findFirstByPlanVersionAndBillingCycle(planVersion.getId(), requestedCycle.name())
                    .orElseThrow(() -> new ResourceNotFoundException("No price found for requested billing cycle"));
        }
        return planPriceRepository.findAllByPlanVersion(planVersion.getId()).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No price configured for active plan version"));
    }

    private Long parsePlanVersionId(String rawPlanVersionId) {
        try {
            return Long.parseLong(rawPlanVersionId);
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Plan version id must be a valid integer", ex);
        }
    }

    private String missingPlanVersionMessage(String rawPlanVersionId, Long parsedPlanVersionId) {
        String message = "Plan version " + rawPlanVersionId + " not found";
        if (parsedPlanVersionId > MAX_SAFE_JAVASCRIPT_INTEGER) {
            return message + ". If this id comes from a JavaScript client, send it as a string to avoid numeric precision loss. Plan code fallback also did not find an active version";
        }
        return message;
    }

    private PlanVersion resolveLatestActiveVersionByPlanCode(String planCode) {
        if (!StringUtils.hasText(planCode)) {
            throw new BadRequestException("Plan code is required");
        }
        SubscriptionPlan plan = planRepository.findByCodeIgnoreCase(planCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan " + planCode + " not found"));
        return resolveLatestActiveVersionByPlan(plan, "No active version found for plan " + planCode);
    }

    private PlanVersion resolveLatestActiveVersionByPlan(SubscriptionPlan plan, String missingMessage) {
        return planVersionRepository.findFirstByPlanAndStatusOrderByVersionNumberDesc(plan.getId(), PlanStatus.ACTIVE.name())
                .orElseThrow(() -> new ResourceNotFoundException(missingMessage));
    }
}
