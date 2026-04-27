package com.sni.bokaticowork.features.subscription.rollover.service.impl;

import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.subscription.repository.EntitlementGrantRepository;
import com.sni.bokaticowork.features.subscription.repository.PlanEntitlementRepository;
import com.sni.bokaticowork.features.subscription.rollover.dto.RolloverRecordResponse;
import com.sni.bokaticowork.features.subscription.rollover.model.SubscriptionRolloverRecord;
import com.sni.bokaticowork.features.subscription.rollover.repository.SubscriptionRolloverRecordRepository;
import com.sni.bokaticowork.features.subscription.rollover.service.SubscriptionRolloverService;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementGrantStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementTransactionType;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementGrant;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanEntitlement;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.support.EntitlementLedgerWriter;
import com.sni.bokaticowork.features.subscription.timeline.enums.SubscriptionTimelineEventType;
import com.sni.bokaticowork.features.subscription.timeline.service.SubscriptionTimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class SubscriptionRolloverServiceImpl implements SubscriptionRolloverService {

    private final EntitlementGrantRepository grantRepository;
    private final PlanEntitlementRepository planEntitlementRepository;
    private final SubscriptionRolloverRecordRepository rolloverRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final EntitlementLedgerWriter ledgerWriter;
    private final SubscriptionTimelineService timelineService;

    @Override
    public int applyDueRollovers() {
        List<EntitlementGrant> candidates = grantRepository.findRolloverCandidates(Instant.now());
        candidates.forEach(this::rollover);
        return candidates.size();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RolloverRecordResponse> list(String subscriptionNumber, String entitlementCode) {
        return rolloverRepository.search(trim(subscriptionNumber), trim(entitlementCode)).stream()
                .map(this::toResponse)
                .toList();
    }

    private void rollover(EntitlementGrant source) {
        Subscription subscription = source.getSubscription();
        PlanEntitlement policy = planEntitlementRepository.findByPlanVersionAndEntitlementDefinition(
                subscription.getPlanVersion().getId(),
                source.getEntitlementDefinition().getId()
        ).orElseThrow();
        BigDecimal quantity = source.getQuantityRemaining();
        if (policy.getRolloverLimit() != null) {
            quantity = quantity.min(policy.getRolloverLimit());
        }
        if (quantity.signum() <= 0) {
            return;
        }

        EntitlementGrant rolloverGrant = grantRepository.save(EntitlementGrant.builder()
                .grantNumber(sequenceGenerator.next("entitlement_grant"))
                .subscription(subscription)
                .entitlementDefinition(source.getEntitlementDefinition())
                .ownerType(source.getOwnerType())
                .ownerCode(source.getOwnerCode())
                .quantityGranted(quantity)
                .quantityRemaining(quantity)
                .unlimited(Boolean.FALSE)
                .validFrom(subscription.getCurrentPeriodStart().atStartOfDay(ZoneId.systemDefault()).toInstant())
                .validUntil(subscription.getCurrentPeriodEnd() == null ? null : subscription.getCurrentPeriodEnd().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant())
                .status(EntitlementGrantStatus.ACTIVE)
                .sourceType("ROLLOVER")
                .sourceId(source.getGrantNumber())
                .priority(Math.max(1, source.getPriority() - 1))
                .build());
        ledgerWriter.write(rolloverGrant, EntitlementTransactionType.ROLLOVER, quantity, BigDecimal.ZERO, quantity, "ENTITLEMENT_GRANT", source.getGrantNumber(), null, "rollover from previous period");

        SubscriptionRolloverRecord record = rolloverRepository.save(SubscriptionRolloverRecord.builder()
                .rolloverNumber(sequenceGenerator.next("subscription_rollover"))
                .subscription(subscription)
                .sourceGrant(source)
                .rolloverGrant(rolloverGrant)
                .entitlementCode(source.getEntitlementDefinition().getCode())
                .quantity(quantity)
                .build());
        timelineService.write(subscription, SubscriptionTimelineEventType.ROLLOVER_APPLIED, "ENTITLEMENT_GRANT", rolloverGrant.getGrantNumber(), "Rollover applied", source.getEntitlementDefinition().getCode(), "{\"rolloverNumber\":\"" + record.getRolloverNumber() + "\"}");
    }

    private RolloverRecordResponse toResponse(SubscriptionRolloverRecord record) {
        return new RolloverRecordResponse(
                record.getRolloverNumber(),
                record.getSubscription().getSubscriptionNumber(),
                record.getSourceGrant().getGrantNumber(),
                record.getRolloverGrant().getGrantNumber(),
                record.getEntitlementCode(),
                record.getQuantity(),
                record.getCreatedAt()
        );
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
