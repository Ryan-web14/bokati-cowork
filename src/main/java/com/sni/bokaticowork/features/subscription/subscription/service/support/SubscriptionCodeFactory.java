package com.sni.bokaticowork.features.subscription.subscription.service.support;

import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.utils.code.CodeComposer;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.CreateEntitlementDefinitionRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.CreatePlanRequest;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class SubscriptionCodeFactory {

    private final SequenceGeneratorFacade sequenceGenerator;

    public String nextPlanCode(CreatePlanRequest request) {
        LocalDate businessDate = LocalDate.now();
        long seq = nextSequence("subscription_plan", businessDate);
        String context = joinContext(
                codeOf(request.planType() == null ? null : request.planType().name()),
                codeOf(request.targetAudience() == null ? null : request.targetAudience().name())
        );
        return CodeComposer.withMonth("PLN", context, businessDate, seq);
    }

    public String nextEntitlementDefinitionCode(CreateEntitlementDefinitionRequest request) {
        LocalDate businessDate = LocalDate.now();
        long seq = nextSequence("entitlement_definition", businessDate);
        String resourceContext = StringUtils.hasText(request.resourceTypeCode())
                ? request.resourceTypeCode()
                : request.resourceGroupCode();
        String context = joinContext(
                codeOf(request.entitlementType() == null ? null : request.entitlementType().name()),
                codeOf(request.unit() == null ? null : request.unit().name()),
                codeOf(request.consumptionMode() == null ? null : request.consumptionMode().name()),
                codeOf(request.resetPolicy() == null ? null : request.resetPolicy().name()),
                codeOf(resourceContext)
        );
        return CodeComposer.withMonth("ENT", context, businessDate, seq);
    }

    public String nextSubscriptionNumber(SubscriberType subscriberType,
                                         PlanVersion planVersion,
                                         BillingCycle billingCycle,
                                         LocalDate businessDate) {
        LocalDate effectiveDate = businessDate == null ? LocalDate.now() : businessDate;
        long seq = nextSequence("subscription", effectiveDate);
        String planType = planVersion != null && planVersion.getPlan() != null && planVersion.getPlan().getPlanType() != null
                ? planVersion.getPlan().getPlanType().name()
                : null;
        String context = joinContext(
                codeOf(subscriberType == null ? null : subscriberType.name()),
                codeOf(planType),
                codeOf(billingCycle == null ? null : billingCycle.name())
        );
        return CodeComposer.withMonth("SUB", context, effectiveDate, seq);
    }

    private long nextSequence(String sequenceCode, LocalDate businessDate) {
        return CodeComposer.extractSeq(sequenceGenerator.next(sequenceCode, businessDate));
    }

    private String joinContext(String... segments) {
        String joined = Arrays.stream(segments)
                .filter(StringUtils::hasText)
                .reduce((left, right) -> left + "-" + right)
                .orElse("GEN");
        return joined.length() > 40 ? joined.substring(0, 40) : joined;
    }

    private String codeOf(String value) {
        return StringUtils.hasText(value) ? CodeComposer.abbrev(value) : null;
    }
}
