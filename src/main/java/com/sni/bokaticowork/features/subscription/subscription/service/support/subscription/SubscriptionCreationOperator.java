package com.sni.bokaticowork.features.subscription.subscription.service.support.subscription;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceAlreadyExistException;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.kyc.KycCaseStatus;
import com.sni.bokaticowork.features.document.kyc.repository.KycCaseRepository;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.CreateSubscriptionRequest;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingScheduleStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionEventType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanPrice;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.model.SubscriptionItem;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionItemRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.service.support.ContractGenerationEvent;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionBillingSupport;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionEventWriter;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionOwnerResolver;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionPeriodCalculator;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionPlanResolver;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionCodeFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class SubscriptionCreationOperator {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionItemRepository subscriptionItemRepository;
    private final SubscriptionOwnerResolver ownerResolver;
    private final SubscriptionPlanResolver planResolver;
    private final SubscriptionPeriodCalculator periodCalculator;
    private final SubscriptionBillingSupport billingSupport;
    private final SubscriptionLifecycleOperator lifecycleOperator;
    private final SubscriptionEventWriter eventWriter;
    private final SubscriptionCodeFactory codeFactory;
    private final KycCaseRepository kycCaseRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final SubscriptionEmailNotifier emailNotifier;

    public Subscription create(CreateSubscriptionRequest request) {
        PlanVersion planVersion = planResolver.resolvePlanVersion(request.planCode(), request.planVersionId());
        PlanPrice price = planResolver.resolvePrice(planVersion, request.billingCycle());
        SubscriptionOwnerResolver.Owner owner = ownerResolver.resolve(request.subscriberType(), request.subscriberCode());
        validateRequiredKycLevel(planVersion, owner);

        var existingSuscritpion = subscriptionRepository.findBySuscriberCodeAndPlanVersion(request.subscriberCode(), planVersion.getId());

        if(!existingSuscritpion.isEmpty()){
            throw new ResourceAlreadyExistException("Subscription already exists for subscriber code: " + request.subscriberCode() + " and plan version: " + planVersion.getId());
        }

        LocalDate startDate = request.startDate();
        String subscriptionNumber = codeFactory.nextSubscriptionNumber(request.subscriberType(), planVersion, price.getBillingCycle(), startDate);

        Subscription subscription = Subscription.builder()
                .subscriptionNumber(subscriptionNumber)
                .subscriberType(request.subscriberType())
                .subscriberCode(owner.code())
                .member(owner.member())
                .customer(owner.customer())
                .businessEntity(owner.businessEntity())
                .planVersion(planVersion)
                .status(SubscriptionStatus.PENDING_ACTIVATION)
                .startDate(startDate)
                .currentPeriodStart(startDate)
                .currentPeriodEnd(periodCalculator.periodEnd(startDate, price.getBillingCycle()))
                .nextBillingDate(periodCalculator.nextBillingDate(startDate, price.getBillingCycle()))
                .autoRenew(request.autoRenew() == null || request.autoRenew())
                .billingCycle(price.getBillingCycle())
                .currency(price.getCurrency())
                .subtotalAmount(price.getAmount())
                .taxAmount(java.math.BigDecimal.ZERO)
                .totalAmount(price.getAmount().add(price.getSetupFee()).add(price.getDepositAmount()))
                .metadataJson(trim(request.metadataJson()))
                .build();

        Subscription saved = subscriptionRepository.save(subscription);
        subscriptionItemRepository.save(SubscriptionItem.builder()
                .subscription(saved)
                .planVersion(planVersion)
                .quantity(1)
                .unitPrice(price.getAmount())
                .currency(price.getCurrency())
                .startDate(startDate)
                .status(saved.getStatus())
                .build());

        // Génération du contrat en asynchrone après commit — ne bloque pas la création
        eventPublisher.publishEvent(ContractGenerationEvent.forSubscription(saved.getId()));

        eventWriter.writeHistory(saved, null, saved.getStatus(), "Creation", "SYSTEM");
        eventWriter.writeEvent(saved, SubscriptionEventType.SUBSCRIPTION_CREATED, null);
        emailNotifier.notify(saved, SubscriptionEventType.SUBSCRIPTION_CREATED);
        billingSupport.upsertBillingSchedule(saved, BillingScheduleStatus.ACTIVE);
        billingSupport.createBillableItem(saved, "SUBSCRIPTION_SETUP", "Initial subscription charge");

        if (saved.getTotalAmount().signum() == 0 || Boolean.TRUE.equals(request.autoActivate())) {
            lifecycleOperator.activate(saved, "Auto activation", "SYSTEM");
        }
        return saved;
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void validateRequiredKycLevel(PlanVersion planVersion, SubscriptionOwnerResolver.Owner owner) {
        Integer requiredLevel = planVersion.getPlan() == null ? null : planVersion.getPlan().getRequiredKycLevel();
        if (requiredLevel == null || requiredLevel <= 1) {
            return;
        }
        OwnerKyc ownerKyc = resolveOwnerKyc(owner);
        int currentLevel = kycCaseRepository.findFirstByOwnerTypeAndOwnerIdOrderByStartedAtDesc(ownerKyc.ownerType(), ownerKyc.ownerId())
                .filter(kycCase -> kycCase.getStatus() == KycCaseStatus.APPROVED)
                .map(kycCase -> kycCase.getKycLevel() == null ? 1 : kycCase.getKycLevel())
                .orElse(1);
        if (currentLevel < requiredLevel) {
            throw new BadRequestException("KYC level " + requiredLevel + " required for this plan");
        }
    }

    private OwnerKyc resolveOwnerKyc(SubscriptionOwnerResolver.Owner owner) {
        if (owner.member() != null) {
            return new OwnerKyc(DocumentOwnerType.MEMBER, owner.member().getId());
        }
        if (owner.customer() != null) {
            return new OwnerKyc(DocumentOwnerType.CUSTOMER, owner.customer().getId());
        }
        if (owner.businessEntity() != null) {
            return new OwnerKyc(DocumentOwnerType.BUSINESS, owner.businessEntity().getId());
        }
        throw new BadRequestException("KYC owner could not be resolved for subscription");
    }

    private record OwnerKyc(DocumentOwnerType ownerType, Long ownerId) {
    }
}
