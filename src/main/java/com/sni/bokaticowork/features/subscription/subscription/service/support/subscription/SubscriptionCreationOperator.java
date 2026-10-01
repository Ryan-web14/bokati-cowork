package com.sni.bokaticowork.features.subscription.subscription.service.support.subscription;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceAlreadyExistException;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.kyc.KycCaseStatus;
import com.sni.bokaticowork.features.document.kyc.repository.KycCaseRepository;
import com.sni.bokaticowork.features.billing.service.support.BillingTaxRuleResolver;
import com.sni.bokaticowork.features.payment.dto.request.CreateWalletHoldRequest;
import com.sni.bokaticowork.features.payment.dto.response.WalletResponse;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletHoldService;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletService;
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
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionEventWriter;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.DiscountDocumentType;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.TargetScope;
import com.sni.bokaticowork.features.subscription.promotion.pricing.service.SubscriptionPricingBridge;
import com.sni.bokaticowork.features.subscription.subscription.service.support.PlanPriceAmountCalculator;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriberKycLevelGuard;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionOwnerResolver;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionPeriodCalculator;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionPlanResolver;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionCodeFactory;
import com.sni.bokaticowork.features.portal.notification.service.MemberInAppNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionCreationOperator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

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
    private final BillingTaxRuleResolver taxRuleResolver;
    private final SubscriptionEmailNotifier emailNotifier;
    private final MemberInAppNotifier memberInAppNotifier;
    private final WalletService walletService;
    private final WalletHoldService walletHoldService;
    private final PlanPriceAmountCalculator priceCalculator;
    private final SubscriberKycLevelGuard kycGuard;
    private final SubscriptionPricingBridge pricingBridge;
    private final com.sni.bokaticowork.features.subscription.lifecycle.service.SubscriptionCommitmentService commitmentService;

    public Subscription create(CreateSubscriptionRequest request) {
        PlanVersion planVersion = planResolver.resolvePlanVersion(request.planCode(), request.planVersionId());
        PlanPrice price = planResolver.resolvePrice(planVersion, request.billingCycle());
        SubscriptionOwnerResolver.Owner owner = ownerResolver.resolve(request.subscriberType(), request.subscriberCode());
        kycGuard.require(planVersion.getPlan() == null ? null : planVersion.getPlan().getRequiredKycLevel(),
                owner, "plan");

        var existingSuscritpion = subscriptionRepository.findBySuscriberCodeAndPlanVersion(request.subscriberCode(), planVersion.getId());

        if(!existingSuscritpion.isEmpty()){
            throw new ResourceAlreadyExistException("Subscription already exists for subscriber code: " + request.subscriberCode() + " and plan version: " + planVersion.getId());
        }

        LocalDate startDate = request.startDate();
        String subscriptionNumber = codeFactory.nextSubscriptionNumber(request.subscriberType(), planVersion, price.getBillingCycle(), startDate);

        // La remise s'applique avant la taxe : celle-ci porte sur ce que le client paie reellement,
        // pas sur un tarif catalogue qu'il ne paie jamais.
        SubscriptionPricingBridge.PricedSubscription priced = pricingBridge.price(
                request.subscriberType().name(), owner.code(), null, TargetScope.PLAN,
                planVersion.getPlan() == null ? request.planCode() : planVersion.getPlan().getCode(),
                price.getBillingCycle() == null ? null : price.getBillingCycle().name(),
                price.getCurrency(), price.getAmount(), price.getSetupFee(),
                request.couponCodesOrEmpty(), true);

        PlanPriceAmountCalculator.Amounts priceAmounts = priceCalculator.compute(
                priced.recurringAmount(), priced.setupFee(), price.getDepositAmount(), price.getTaxIncluded());

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
                .subtotalAmount(priceAmounts.subtotal())
                .taxAmount(priceAmounts.tax())
                .totalAmount(priceAmounts.total())
                .metadataJson(trim(request.metadataJson()))
                .build();

        Subscription saved = subscriptionRepository.save(subscription);
        createDepositHold(saved, price, owner);
        subscriptionItemRepository.save(SubscriptionItem.builder()
                .subscription(saved)
                .planVersion(planVersion)
                .quantity(1)
                .unitPrice(price.getAmount())
                .currency(price.getCurrency())
                .startDate(startDate)
                .status(saved.getStatus())
                .build());

        eventWriter.writeHistory(saved, null, saved.getStatus(), "Creation", "SYSTEM");
        eventWriter.writeEvent(saved, SubscriptionEventType.SUBSCRIPTION_CREATED, null);
        // Le prix de plan porte un engagement · il est pose ici, avec la formule de rupture de la politique
        commitmentService.setFromPlan(saved, price.getCommitmentMonths());
        notifyInApp(saved, SubscriptionEventType.SUBSCRIPTION_CREATED);
        billingSupport.upsertBillingSchedule(saved, BillingScheduleStatus.ACTIVE);
        billingSupport.createSubscriptionSetupItems(saved, priced.recurringAmount(), priced.setupFee(),
                price.getDepositAmount() == null ? java.math.BigDecimal.ZERO : price.getDepositAmount());
        pricingBridge.confirm(priced, request.subscriberType().name(), owner.code(), null, TargetScope.PLAN,
                planVersion.getPlan() == null ? request.planCode() : planVersion.getPlan().getCode(),
                price.getBillingCycle() == null ? null : price.getBillingCycle().name(),
                price.getCurrency(), request.couponCodesOrEmpty(),
                DiscountDocumentType.SUBSCRIPTION, saved.getSubscriptionNumber(), "SYSTEM");

        if (saved.getTotalAmount().signum() == 0 || Boolean.TRUE.equals(request.autoActivate())) {
            lifecycleOperator.activate(saved, "Auto activation", "SYSTEM");
        } else {
            // Pas d'auto-activation · envoyer l'email de création en attente de paiement
            emailNotifier.notify(saved, SubscriptionEventType.SUBSCRIPTION_CREATED);
        }
        return saved;
    }

    private void createDepositHold(Subscription subscription, PlanPrice price, SubscriptionOwnerResolver.Owner owner) {
        if (price.getDepositAmount() == null || price.getDepositAmount().signum() <= 0) {
            return;
        }
        try {
            WalletResponse wallet = walletService.getOrCreate(subscription.getSubscriberType().name(), owner.code(), price.getCurrency());
            walletHoldService.create(new CreateWalletHoldRequest(wallet.walletNumber(), price.getDepositAmount(),
                    "SUBSCRIPTION", subscription.getSubscriptionNumber(), null, "SYSTEM"));
        } catch (Exception ex) {
            log.warn("Failed to place deposit hold for subscription {}", subscription.getSubscriptionNumber(), ex);
        }
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void notifyInApp(Subscription subscription, SubscriptionEventType eventType) {
        String email = resolveRecipientEmail(subscription);
        String name = resolveRecipientName(subscription);
        String subject = "Abonnement cree " + subscription.getSubscriptionNumber();
        memberInAppNotifier.notify(
                eventType.name(), "SUBSCRIPTION", subscription.getSubscriptionNumber(),
                email, name, subscription.getSubscriberCode(), subject,
                java.util.Map.of(
                        "subscriptionNumber", subscription.getSubscriptionNumber(),
                        "status", subscription.getStatus() != null ? subscription.getStatus().name() : "",
                        "planName", subscription.getPlanVersion() != null ? subscription.getPlanVersion().getName() : ""
                )
        );
    }

    private String resolveRecipientEmail(Subscription subscription) {
        if (subscription.getMember() != null) return subscription.getMember().getEmail();
        if (subscription.getCustomer() != null) {
            return StringUtils.hasText(subscription.getCustomer().getBillingEmail())
                    ? subscription.getCustomer().getBillingEmail() : subscription.getCustomer().getEmail();
        }
        if (subscription.getBusinessEntity() != null) return subscription.getBusinessEntity().getEmail();
        return null;
    }

    private String resolveRecipientName(Subscription subscription) {
        if (subscription.getMember() != null) return subscription.getMember().getDisplayName();
        if (subscription.getCustomer() != null) return subscription.getCustomer().getFirstname();
        if (subscription.getBusinessEntity() != null) return subscription.getBusinessEntity().getName();
        return subscription.getSubscriberCode();
    }
}
