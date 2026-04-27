package com.sni.bokaticowork.features.subscription.usage.mapper.decorator;

import com.sni.bokaticowork.features.subscription.subscription.dto.request.EntitlementOperationRequest;
import com.sni.bokaticowork.features.subscription.usage.dto.CreateUsageRecordRequest;
import com.sni.bokaticowork.features.subscription.usage.dto.UsageRecordResponse;
import com.sni.bokaticowork.features.subscription.usage.mapper.interfaces.UsageRecordMapper;
import com.sni.bokaticowork.features.subscription.usage.model.UsageRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public abstract class UsageRecordMapperDecorator implements UsageRecordMapper {

    @Autowired
    @Qualifier("delegate")
    private UsageRecordMapper delegate;

    @Override
    public UsageRecord toEntity(CreateUsageRecordRequest request) {
        UsageRecord usage = delegate.toEntity(request);
        usage.setOwnerCode(request.ownerCode().trim());
        usage.setEntitlementCode(request.entitlementCode().trim());
        usage.setReferenceType(request.referenceType().trim());
        usage.setReferenceId(request.referenceId().trim());
        usage.setBillable(Boolean.TRUE.equals(request.billable()));
        usage.setMetadataJson(trim(request.metadataJson()));
        return usage;
    }

    @Override
    public UsageRecordResponse toResponse(UsageRecord usage) {
        return new UsageRecordResponse(
                usage.getUsageNumber(),
                usage.getOwnerType(),
                usage.getOwnerCode(),
                usage.getEntitlementCode(),
                usage.getQuantity(),
                usage.getUnit(),
                usage.getReferenceType(),
                usage.getReferenceId(),
                usage.getBillable(),
                usage.getBillableItem() == null ? null : usage.getBillableItem().getBillableNumber(),
                usage.getStatus(),
                usage.getOccurredAt(),
                usage.getMetadataJson()
        );
    }

    @Override
    public EntitlementOperationRequest toEntitlementOperation(CreateUsageRecordRequest request) {
        String referenceType = request.referenceType().trim();
        String referenceId = request.referenceId().trim();
        String entitlementCode = request.entitlementCode().trim();
        return new EntitlementOperationRequest(
                request.ownerType(),
                request.ownerCode().trim(),
                entitlementCode,
                request.quantity(),
                referenceType,
                referenceId,
                "USAGE:" + referenceType + ":" + referenceId + ":" + entitlementCode,
                "usage record"
        );
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
