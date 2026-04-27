package com.sni.bokaticowork.features.subscription.subscription.service.support.subscription;

import com.sni.bokaticowork.core.exception.customs.ResourceAlreadyExistException;
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
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionBillingSupport;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionContractSupport;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionEventWriter;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionOwnerResolver;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionPeriodCalculator;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionPlanResolver;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionCodeFactory;
import lombok.RequiredArgsConstructor;
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
    private final SubscriptionContractSupport contractSupport;
    private final SubscriptionCodeFactory codeFactory;

    public Subscription create(CreateSubscriptionRequest request) {
        PlanVersion planVersion = planResolver.resolvePlanVersion(request.planCode(), request.planVersionId());
        PlanPrice price = planResolver.resolvePrice(planVersion, request.billingCycle());
        SubscriptionOwnerResolver.Owner owner = ownerResolver.resolve(request.subscriberType(), request.subscriberCode());

        var existingSuscritpion = subscriptionRepository.findBySuscriberCodeAndPlanVersion(request.subscriberCode(), planVersion.getId());

        if(existingSuscritpion != null){
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

        saved.setContractCode(contractSupport.createAndSignForSubscription(saved));
        saved = subscriptionRepository.save(saved);

        eventWriter.writeHistory(saved, null, saved.getStatus(), "Creation", "SYSTEM");
        eventWriter.writeEvent(saved, SubscriptionEventType.SUBSCRIPTION_CREATED, null);
        billingSupport.upsertBillingSchedule(saved, BillingScheduleStatus.ACTIVE);
        billingSupport.createBillableItem(saved, "SUBSCRIPTION_SETUP", "Initial subscription charge");

        if (saved.getTotalAmount().signum() == 0) {
            lifecycleOperator.activate(saved, "Auto activation for zero amount", "SYSTEM");
        }
        return saved;
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
