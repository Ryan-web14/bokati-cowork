package com.sni.bokaticowork.features.subscription.subscription.service.support;

import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementGrantStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementTransactionType;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementGrant;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.model.PassEntitlement;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanEntitlement;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.repository.EntitlementGrantRepository;
import com.sni.bokaticowork.features.subscription.repository.PassEntitlementRepository;
import com.sni.bokaticowork.features.subscription.repository.PlanEntitlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EntitlementGrantIssuer {

    private final EntitlementGrantRepository grantRepository;
    private final PlanEntitlementRepository planEntitlementRepository;
    private final PassEntitlementRepository passEntitlementRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final EntitlementLedgerWriter ledgerWriter;

    public void grantForSubscription(Subscription subscription) {
        List<PlanEntitlement> entitlements = planEntitlementRepository.findAllByPlanVersionOrderByPriorityAsc(subscription.getPlanVersion().getId());
        Instant validFrom = startOfDay(subscription.getCurrentPeriodStart() == null ? subscription.getStartDate() : subscription.getCurrentPeriodStart());
        Instant validUntil = subscription.getCurrentPeriodEnd() == null ? null : startOfDay(subscription.getCurrentPeriodEnd().plusDays(1));
        Instant now = Instant.now();

        for (PlanEntitlement entitlement : entitlements) {
            if (grantRepository.findActiveSubscriptionGrantForPeriod(
                    subscription.getId(),
                    entitlement.getEntitlementDefinition().getId(),
                    validFrom,
                    now
            ).isPresent()) {
                continue;
            }

            EntitlementGrant grant = grantRepository.save(EntitlementGrant.builder()
                    .grantNumber(sequenceGenerator.next("entitlement_grant"))
                    .subscription(subscription)
                    .entitlementDefinition(entitlement.getEntitlementDefinition())
                    .ownerType(subscription.getSubscriberType())
                    .ownerCode(subscription.getSubscriberCode())
                    .quantityGranted(entitlement.getQuantity())
                    .quantityRemaining(entitlement.getQuantity())
                    .unlimited(entitlement.getUnlimited())
                    .validFrom(validFrom)
                    .validUntil(resolveValidUntil(entitlement, validFrom, validUntil))
                    .status(EntitlementGrantStatus.ACTIVE)
                    .sourceType("SUBSCRIPTION")
                    .sourceId(subscription.getSubscriptionNumber())
                    .priority(entitlement.getPriority())
                    .build());
            ledgerWriter.write(grant, EntitlementTransactionType.GRANT, entitlement.getQuantity(), BigDecimal.ZERO, entitlement.getQuantity(), "SUBSCRIPTION", subscription.getSubscriptionNumber(), null, "subscription entitlement grant");
        }
    }

    public void grantForPass(Pass pass) {
        List<PassEntitlement> entitlements = passEntitlementRepository.findAllByPass(pass.getId());
        for (PassEntitlement entitlement : entitlements) {
            EntitlementGrant grant = grantRepository.save(EntitlementGrant.builder()
                    .grantNumber(sequenceGenerator.next("entitlement_grant"))
                    .pass(pass)
                    .subscription(pass.getSubscription())
                    .entitlementDefinition(entitlement.getEntitlementDefinition())
                    .ownerType(pass.getOwnerType())
                    .ownerCode(pass.getOwnerCode())
                    .quantityGranted(entitlement.getQuantity())
                    .quantityRemaining(entitlement.getQuantity())
                    .unlimited(entitlement.getUnlimited())
                    .validFrom(entitlement.getValidFrom())
                    .validUntil(entitlement.getValidUntil())
                    .status(EntitlementGrantStatus.ACTIVE)
                    .sourceType("PASS")
                    .sourceId(pass.getPassNumber())
                    .priority(50)
                    .build());
            ledgerWriter.write(grant, EntitlementTransactionType.GRANT, entitlement.getQuantity(), BigDecimal.ZERO, entitlement.getQuantity(), "PASS", pass.getPassNumber(), null, "pass entitlement grant");
        }
    }

    private Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private Instant resolveValidUntil(PlanEntitlement entitlement, Instant validFrom, Instant subscriptionValidUntil) {
        Integer validForDays = entitlement.getValidForDays();
        if (validForDays == null || validForDays <= 0) {
            return subscriptionValidUntil;
        }
        return validFrom.plusSeconds(validForDays.longValue() * 86_400);
    }
}
