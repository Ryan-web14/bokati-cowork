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
import com.sni.bokaticowork.features.subscription.subscription.service.support.ContractGenerationEvent;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionBillingSupport;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionEventWriter;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionOwnerResolver;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionPeriodCalculator;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionPlanResolver;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionCodeFactory;
import com.sni.bokaticowork.features.portal.notification.service.MemberInAppNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;
    private final SubscriptionEmailNotifier emailNotifier;
    private final MemberInAppNotifier memberInAppNotifier;
    private final WalletService walletService;
    private final WalletHoldService walletHoldService;

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
        PriceAmounts priceAmounts = calculatePriceAmounts(price);

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
                .subtotalAmount(priceAmounts.subtotalAmount())
                .taxAmount(priceAmounts.taxAmount())
                .totalAmount(priceAmounts.totalAmount())
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

        // Génération du contrat en asynchrone après commit — ne bloque pas la création
        eventPublisher.publishEvent(ContractGenerationEvent.forSubscription(saved.getId()));

        eventWriter.writeHistory(saved, null, saved.getStatus(), "Creation", "SYSTEM");
        eventWriter.writeEvent(saved, SubscriptionEventType.SUBSCRIPTION_CREATED, null);
        notifyInApp(saved, SubscriptionEventType.SUBSCRIPTION_CREATED);
        billingSupport.upsertBillingSchedule(saved, BillingScheduleStatus.ACTIVE);
        billingSupport.createBillableItem(saved, "SUBSCRIPTION_SETUP", "Initial subscription charge");

        if (saved.getTotalAmount().signum() == 0 || Boolean.TRUE.equals(request.autoActivate())) {
            lifecycleOperator.activate(saved, "Auto activation", "SYSTEM");
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

    private PriceAmounts calculatePriceAmounts(PlanPrice price) {
        BigDecimal recurringAmount = money(nonNegative(price.getAmount()));
        BigDecimal setupFee = money(nonNegative(price.getSetupFee()));
        BigDecimal depositAmount = money(nonNegative(price.getDepositAmount()));
        BigDecimal taxableCharge = money(recurringAmount.add(setupFee));
        BillingTaxRuleResolver.TaxProfile taxProfile = taxRuleResolver.defaultTaxProfile();
        if (Boolean.TRUE.equals(price.getTaxIncluded())) {
            BigDecimal subtotal = money(taxableCharge.divide(taxFactor(taxProfile.vatRate(), taxProfile.additionalCentRate()), 8, RoundingMode.HALF_UP));
            BigDecimal tax = money(taxableCharge.subtract(subtotal));
            return new PriceAmounts(subtotal, tax, money(taxableCharge.add(depositAmount)));
        }
        BigDecimal vatAmount = percentage(taxableCharge, taxProfile.vatRate());
        BigDecimal additionalCentAmount = percentage(vatAmount, taxProfile.additionalCentRate());
        BigDecimal tax = money(vatAmount.add(additionalCentAmount));
        return new PriceAmounts(taxableCharge, tax, money(taxableCharge.add(tax).add(depositAmount)));
    }

    private BigDecimal percentage(BigDecimal amount, BigDecimal rate) {
        if (rate == null || rate.signum() == 0) {
            return BigDecimal.ZERO;
        }
        if (rate.signum() < 0) {
            throw new BadRequestException("Percentage rate cannot be negative");
        }
        return money(amount.multiply(rate).divide(HUNDRED, 4, RoundingMode.HALF_UP));
    }

    private BigDecimal taxFactor(BigDecimal vatRate, BigDecimal additionalCentRate) {
        BigDecimal vatFactor = rateFactor(vatRate);
        BigDecimal additionalCentFactor = rateFactor(additionalCentRate);
        return BigDecimal.ONE.add(vatFactor).add(vatFactor.multiply(additionalCentFactor));
    }

    private BigDecimal rateFactor(BigDecimal rate) {
        if (rate == null || rate.signum() == 0) {
            return BigDecimal.ZERO;
        }
        if (rate.signum() < 0) {
            throw new BadRequestException("Percentage rate cannot be negative");
        }
        return rate.divide(HUNDRED, 8, RoundingMode.HALF_UP);
    }

    private BigDecimal nonNegative(BigDecimal value) {
        BigDecimal candidate = value == null ? BigDecimal.ZERO : value;
        if (candidate.signum() < 0) {
            throw new BadRequestException("Plan price amounts cannot be negative");
        }
        return candidate;
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(4, RoundingMode.HALF_UP);
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

    private record PriceAmounts(BigDecimal subtotalAmount,
                                BigDecimal taxAmount,
                                BigDecimal totalAmount) {
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
