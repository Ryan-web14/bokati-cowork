package com.sni.bokaticowork.features.subscription.overage.service.impl;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.service.support.BillableItemInvoiceSupport;
import com.sni.bokaticowork.features.subscription.overage.dto.CreateOveragePolicyRequest;
import com.sni.bokaticowork.features.subscription.overage.dto.OverageBillingResult;
import com.sni.bokaticowork.features.subscription.overage.dto.OverageChargeResponse;
import com.sni.bokaticowork.features.subscription.overage.dto.OveragePolicyResponse;
import com.sni.bokaticowork.features.subscription.overage.enums.OveragePolicyMode;
import com.sni.bokaticowork.features.subscription.overage.mapper.interfaces.SubscriptionOverageMapper;
import com.sni.bokaticowork.features.subscription.overage.model.SubscriptionOverageCharge;
import com.sni.bokaticowork.features.subscription.overage.model.SubscriptionOveragePolicy;
import com.sni.bokaticowork.features.subscription.overage.repository.SubscriptionOverageChargeRepository;
import com.sni.bokaticowork.features.subscription.overage.repository.SubscriptionOveragePolicyRepository;
import com.sni.bokaticowork.features.subscription.overage.service.SubscriptionOverageService;
import com.sni.bokaticowork.features.subscription.repository.BillableItemRepository;
import com.sni.bokaticowork.features.subscription.repository.EntitlementDefinitionRepository;
import com.sni.bokaticowork.features.subscription.repository.PlanVersionRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.EntitlementOperationRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.EntitlementOperationResponse;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillableItemStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementDefinition;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.EntitlementService;
import com.sni.bokaticowork.features.subscription.timeline.enums.SubscriptionTimelineEventType;
import com.sni.bokaticowork.features.subscription.timeline.service.SubscriptionTimelineService;
import com.sni.bokaticowork.features.subscription.usage.dto.CreateUsageRecordRequest;
import com.sni.bokaticowork.features.subscription.usage.mapper.interfaces.UsageRecordMapper;
import com.sni.bokaticowork.features.subscription.usage.model.UsageRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class SubscriptionOverageServiceImpl implements SubscriptionOverageService {

    private final SubscriptionOveragePolicyRepository policyRepository;
    private final SubscriptionOverageChargeRepository chargeRepository;
    private final PlanVersionRepository planVersionRepository;
    private final EntitlementDefinitionRepository entitlementDefinitionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final BillableItemRepository billableItemRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final SubscriptionOverageMapper overageMapper;
    private final UsageRecordMapper usageRecordMapper;
    private final EntitlementService entitlementService;
    private final SubscriptionTimelineService timelineService;
    private final BillableItemInvoiceSupport billableItemInvoiceSupport;

    @Override
    public OveragePolicyResponse createPolicy(CreateOveragePolicyRequest request) {
        PlanVersion planVersion = planVersionRepository.findById(request.planVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan version not found"));
        EntitlementDefinition definition = entitlementDefinitionRepository.findByCodeIgnoreCase(request.entitlementCode().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Entitlement definition not found"));
        SubscriptionOveragePolicy policy = overageMapper.toEntity(request);
        policy.setPlanVersion(planVersion);
        policy.setEntitlementDefinition(definition);
        return overageMapper.toResponse(policyRepository.save(policy));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OveragePolicyResponse> listPolicies(Long planVersionId, String entitlementCode) {
        return policyRepository.search(planVersionId, trim(entitlementCode)).stream()
                .map(overageMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OverageChargeResponse> listCharges(String subscriptionNumber, SubscriberType ownerType, String ownerCode, String entitlementCode) {
        return chargeRepository.search(trim(subscriptionNumber), ownerType == null ? null : ownerType.name(), trim(ownerCode), trim(entitlementCode)).stream()
                .map(overageMapper::toResponse)
                .toList();
    }

    @Override
    public OverageBillingResult processUsage(CreateUsageRecordRequest request) {
        if (Boolean.FALSE.equals(request.consumeEntitlement())) {
            return OverageBillingResult.none();
        }

        EntitlementOperationRequest operation = usageRecordMapper.toEntitlementOperation(request);
        EntitlementOperationResponse check = entitlementService.check(operation);
        if (check.allowed()) {
            entitlementService.consume(operation);
            return OverageBillingResult.none();
        }

        Subscription subscription = subscriptionRepository.findCurrentActive(request.ownerType().name(), request.ownerCode().trim(), LocalDate.now())
                .orElseThrow(() -> new ConflictException("entitlement", "insufficient balance and no active subscription for overage billing"));
        SubscriptionOveragePolicy policy = policyRepository.findActivePolicy(subscription.getPlanVersion().getId(), request.entitlementCode().trim())
                .orElseThrow(() -> new ConflictException("entitlement", "insufficient balance and no overage policy"));

        if (policy.getMode() == OveragePolicyMode.BLOCK) {
            throw new ConflictException("entitlement", "insufficient balance and overage is blocked");
        }

        BigDecimal available = check.availableQuantity() == null ? BigDecimal.ZERO : check.availableQuantity();
        if (available.signum() > 0) {
            entitlementService.consume(new EntitlementOperationRequest(
                    operation.ownerType(),
                    operation.ownerCode(),
                    operation.entitlementCode(),
                    available,
                    operation.referenceType(),
                    operation.referenceId(),
                    operation.idempotencyKey() + ":PARTIAL",
                    "partial entitlement consumption before overage"
            ));
        }

        BigDecimal overageQuantity = request.quantity().subtract(available).max(BigDecimal.ZERO);
        BigDecimal billableQuantity = overageQuantity.subtract(policy.getFreeQuantity() == null ? BigDecimal.ZERO : policy.getFreeQuantity()).max(BigDecimal.ZERO);
        if (policy.getMode() == OveragePolicyMode.ALLOW_UNBILLED || billableQuantity.signum() == 0) {
            return new OverageBillingResult(true, false, available, overageQuantity, null, subscription);
        }

        BigDecimal amount = billableQuantity.multiply(policy.getUnitPrice() == null ? BigDecimal.ZERO : policy.getUnitPrice());
        BillableItem billableItem = billableItemRepository.save(BillableItem.builder()
                .billableNumber(sequenceGenerator.next("billable_item"))
                .sourceType("USAGE_OVERAGE")
                .sourceId(request.referenceId().trim())
                .subscriberType(request.ownerType())
                .subscriberCode(request.ownerCode().trim())
                .description("Usage overage - " + request.entitlementCode().trim())
                .amount(amount)
                .currency(policy.getCurrency())
                .status(BillableItemStatus.PENDING)
                .build());
        billableItemInvoiceSupport.ensureInvoiced(
                billableItem,
                "Facture overage " + request.referenceId().trim(),
                "Facture generee automatiquement pour le depassement " + request.entitlementCode().trim()
        );

        return new OverageBillingResult(true, true, available, billableQuantity, billableItem, subscription);
    }

    @Override
    public SubscriptionOverageCharge createCharge(UsageRecord usage, OverageBillingResult result) {
        if (!result.billable() || result.billableItem() == null || result.overageQuantity().signum() <= 0) {
            return null;
        }
        BigDecimal unitPrice = result.billableItem().getAmount().divide(result.overageQuantity(), 4, RoundingMode.HALF_UP);
        SubscriptionOverageCharge charge = chargeRepository.save(SubscriptionOverageCharge.builder()
                .chargeNumber(sequenceGenerator.next("subscription_overage_charge"))
                .subscription(result.subscription())
                .usageRecord(usage)
                .billableItem(result.billableItem())
                .ownerType(usage.getOwnerType())
                .ownerCode(usage.getOwnerCode())
                .entitlementCode(usage.getEntitlementCode())
                .overageQuantity(result.overageQuantity())
                .unitPrice(unitPrice)
                .amount(result.billableItem().getAmount())
                .currency(result.billableItem().getCurrency())
                .build());
        timelineService.write(result.subscription(), SubscriptionTimelineEventType.OVERAGE_BILLED, "USAGE", usage.getUsageNumber(), "Overage billed", usage.getEntitlementCode(), "{\"chargeNumber\":\"" + charge.getChargeNumber() + "\"}");
        return charge;
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
