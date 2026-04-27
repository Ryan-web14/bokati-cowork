package com.sni.bokaticowork.features.subscription.usage.mapper.interfaces;

import com.sni.bokaticowork.features.subscription.subscription.dto.request.EntitlementOperationRequest;
import com.sni.bokaticowork.features.subscription.usage.dto.CreateUsageRecordRequest;
import com.sni.bokaticowork.features.subscription.usage.dto.UsageRecordResponse;
import com.sni.bokaticowork.features.subscription.usage.mapper.decorator.UsageRecordMapperDecorator;
import com.sni.bokaticowork.features.subscription.usage.model.UsageRecord;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(UsageRecordMapperDecorator.class)
public interface UsageRecordMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usageNumber", ignore = true)
    @Mapping(target = "billableItem", ignore = true)
    @Mapping(target = "status", ignore = true)
    UsageRecord toEntity(CreateUsageRecordRequest request);

    UsageRecordResponse toResponse(UsageRecord usage);

    EntitlementOperationRequest toEntitlementOperation(CreateUsageRecordRequest request);
}
