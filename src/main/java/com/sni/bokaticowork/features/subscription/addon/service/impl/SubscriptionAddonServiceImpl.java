package com.sni.bokaticowork.features.subscription.addon.service.impl;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.billing.service.support.BillableItemInvoiceSupport;
import com.sni.bokaticowork.features.subscription.addon.dto.CreateSubscriptionAddonRequest;
import com.sni.bokaticowork.features.subscription.addon.dto.SubscriptionAddonResponse;
import com.sni.bokaticowork.features.subscription.addon.enums.SubscriptionAddonStatus;
import com.sni.bokaticowork.features.subscription.addon.mapper.interfaces.SubscriptionAddonMapper;
import com.sni.bokaticowork.features.subscription.addon.model.SubscriptionAddon;
import com.sni.bokaticowork.features.subscription.addon.repository.SubscriptionAddonRepository;
import com.sni.bokaticowork.features.subscription.addon.repository.specification.SubscriptionAddonCriteria;
import com.sni.bokaticowork.features.subscription.addon.repository.specification.SubscriptionAddonSpecification;
import com.sni.bokaticowork.features.subscription.addon.service.SubscriptionAddonService;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillableItemStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanPrice;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.repository.BillableItemRepository;
import com.sni.bokaticowork.features.subscription.repository.PlanPriceRepository;
import com.sni.bokaticowork.features.subscription.repository.PlanVersionRepository;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.SubscriptionService;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionContractSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@Transactional
@RequiredArgsConstructor
public class SubscriptionAddonServiceImpl implements SubscriptionAddonService {

    private final SubscriptionAddonRepository addonRepository;
    private final SubscriptionService subscriptionService;
    private final PlanVersionRepository planVersionRepository;
    private final PlanPriceRepository planPriceRepository;
    private final BillableItemRepository billableItemRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final SubscriptionAddonMapper addonMapper;
    private final SubscriptionContractSupport contractSupport;
    private final BillableItemInvoiceSupport billableItemInvoiceSupport;

    @Override
    public SubscriptionAddonResponse add(String subscriptionNumber, CreateSubscriptionAddonRequest request) {
        Subscription subscription = subscriptionService.getForService(subscriptionNumber);
        PlanVersion planVersion = planVersionRepository.findById(request.planVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan version not found"));
        PlanPrice price = planPriceRepository.findAllByPlanVersion(planVersion.getId()).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No price found for addon plan version"));

        LocalDate startsAt = request.startsAt() == null ? LocalDate.now() : request.startsAt();
        if (request.endsAt() != null && request.endsAt().isBefore(startsAt)) {
            throw new BadRequestException("Addon end date cannot be before start date");
        }
        SubscriptionAddon addon = addonMapper.toEntity(request);
        addon.setSubscription(subscription);
        addon.setPlanVersion(planVersion);
        addon.setUnitPrice(price.getAmount());
        addon.setCurrency(price.getCurrency());
        addon = addonRepository.save(addon);
        addon.setContractCode(contractSupport.createAndSignForAddon(addon));
        addon = addonRepository.save(addon);

        createBillableItem(subscription, addon, price.getAmount().multiply(BigDecimal.valueOf(addon.getQuantity())));
        return addonMapper.toResponse(addon);
    }

    @Override
    public SubscriptionAddonResponse cancel(Long addonId) {
        SubscriptionAddon addon = addonRepository.findById(addonId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription addon not found"));
        addon.setStatus(SubscriptionAddonStatus.CANCELLED);
        addon.setEndsAt(LocalDate.now());
        return addonMapper.toResponse(addonRepository.save(addon));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<SubscriptionAddonResponse> list(String subscriptionNumber, String planCode, SubscriptionAddonStatus status, Pageable pageable) {
        return new PaginatedResponse<>(addonRepository.findAll(
                SubscriptionAddonSpecification.search(SubscriptionAddonCriteria.builder()
                        .subscriptionNumber(subscriptionNumber)
                        .planCode(planCode)
                        .status(status)
                        .build()),
                pageable
        ).map(addonMapper::toResponse));
    }

    @Override
    public int expireEndedAddons() {
        return addonRepository.expireEndedAddons(LocalDate.now());
    }

    private void createBillableItem(Subscription subscription, SubscriptionAddon addon, BigDecimal amount) {
        if (amount.signum() <= 0) {
            return;
        }
        BillableItem item = billableItemRepository.save(BillableItem.builder()
                .billableNumber(sequenceGenerator.next("billable_item"))
                .sourceType("SUBSCRIPTION_ADDON")
                .sourceId(String.valueOf(addon.getId()))
                .subscriberType(subscription.getSubscriberType())
                .subscriberCode(subscription.getSubscriberCode())
                .description("Subscription addon - " + addon.getPlanVersion().getName())
                .amount(amount)
                .currency(addon.getCurrency())
                .billingPeriodStart(addon.getStartsAt())
                .billingPeriodEnd(addon.getEndsAt())
                .status(BillableItemStatus.PENDING)
                .build());
        billableItemInvoiceSupport.ensureInvoiced(
                item,
                "Facture addon abonnement " + subscription.getSubscriptionNumber(),
                "Facture generee automatiquement pour l'addon " + addon.getPlanVersion().getName()
        );
    }

}
