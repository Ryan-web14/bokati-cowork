package com.sni.bokaticowork.features.subscription.usage.service.impl;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.billing.service.support.BillableItemInvoiceSupport;
import com.sni.bokaticowork.features.subscription.overage.dto.OverageBillingResult;
import com.sni.bokaticowork.features.subscription.overage.service.SubscriptionOverageService;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillableItemStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import com.sni.bokaticowork.features.subscription.repository.BillableItemRepository;
import com.sni.bokaticowork.features.subscription.timeline.enums.SubscriptionTimelineEventType;
import com.sni.bokaticowork.features.subscription.timeline.service.SubscriptionTimelineService;
import com.sni.bokaticowork.features.subscription.usage.dto.CreateUsageRecordRequest;
import com.sni.bokaticowork.features.subscription.usage.dto.UsageRecordResponse;
import com.sni.bokaticowork.features.subscription.usage.enums.UsageRecordStatus;
import com.sni.bokaticowork.features.subscription.usage.mapper.interfaces.UsageRecordMapper;
import com.sni.bokaticowork.features.subscription.usage.model.UsageRecord;
import com.sni.bokaticowork.features.subscription.usage.repository.UsageRecordRepository;
import com.sni.bokaticowork.features.subscription.usage.repository.specification.UsageRecordCriteria;
import com.sni.bokaticowork.features.subscription.usage.repository.specification.UsageRecordSpecification;
import com.sni.bokaticowork.features.subscription.usage.service.UsageRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
@RequiredArgsConstructor
public class UsageRecordServiceImpl implements UsageRecordService {

    private final UsageRecordRepository usageRecordRepository;
    private final BillableItemRepository billableItemRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final UsageRecordMapper usageRecordMapper;
    private final SubscriptionOverageService overageService;
    private final SubscriptionTimelineService timelineService;
    private final BillableItemInvoiceSupport billableItemInvoiceSupport;

    @Override
    public UsageRecordResponse record(CreateUsageRecordRequest request) {
        OverageBillingResult overage = overageService.processUsage(request);
        BillableItem billableItem = overage.billableItem() != null
                ? overage.billableItem()
                : Boolean.TRUE.equals(request.billable()) ? createBillableItem(request) : null;
        UsageRecord usage = usageRecordMapper.toEntity(request);
        usage.setUsageNumber(sequenceGenerator.next("usage_record"));
        usage.setBillableItem(billableItem);
        usage.setStatus(billableItem == null ? UsageRecordStatus.RECORDED : UsageRecordStatus.BILLED);
        UsageRecord saved = usageRecordRepository.save(usage);
        overageService.createCharge(saved, overage);
        timelineService.write(overage.subscription(), SubscriptionTimelineEventType.USAGE_RECORDED, "USAGE", saved.getUsageNumber(), "Usage recorded", saved.getEntitlementCode(), null);
        return usageRecordMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<UsageRecordResponse> list(SubscriberType ownerType, String ownerCode, String entitlementCode, String referenceType, String referenceId, UsageRecordStatus status, Pageable pageable) {
        return new PaginatedResponse<>(usageRecordRepository.findAll(
                UsageRecordSpecification.search(UsageRecordCriteria.builder()
                        .ownerType(ownerType)
                        .ownerCode(ownerCode)
                        .entitlementCode(entitlementCode)
                        .referenceType(referenceType)
                        .referenceId(referenceId)
                        .status(status)
                        .build()),
                pageable
        ).map(usageRecordMapper::toResponse));
    }

    private BillableItem createBillableItem(CreateUsageRecordRequest request) {
        if (request.billableAmount() == null || request.billableAmount().signum() <= 0 || !StringUtils.hasText(request.currency())) {
            throw new BadRequestException("Billable usage requires positive amount and currency");
        }
        BillableItem item = billableItemRepository.save(BillableItem.builder()
                .billableNumber(sequenceGenerator.next("billable_item"))
                .sourceType("USAGE_OVERAGE")
                .sourceId(request.referenceId())
                .subscriberType(request.ownerType())
                .subscriberCode(request.ownerCode())
                .description("Usage overage - " + request.entitlementCode())
                .amount(request.billableAmount())
                .currency(request.currency().trim().toUpperCase())
                .status(BillableItemStatus.PENDING)
                .build());
        billableItemInvoiceSupport.ensureInvoiced(
                item,
                "Facture usage " + request.referenceId(),
                "Facture generee automatiquement pour l'usage billable " + request.entitlementCode()
        );
        return item;
    }

}
