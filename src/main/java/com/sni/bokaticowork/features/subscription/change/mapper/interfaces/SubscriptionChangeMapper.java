package com.sni.bokaticowork.features.subscription.change.mapper.interfaces;

import com.sni.bokaticowork.features.subscription.change.dto.CreateSubscriptionChangeRequest;
import com.sni.bokaticowork.features.subscription.change.dto.SubscriptionChangeResponse;
import com.sni.bokaticowork.features.subscription.change.mapper.decorator.SubscriptionChangeMapperDecorator;
import com.sni.bokaticowork.features.subscription.change.model.SubscriptionChangeRequest;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(SubscriptionChangeMapperDecorator.class)
public interface SubscriptionChangeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "changeNumber", ignore = true)
    @Mapping(target = "subscription", ignore = true)
    @Mapping(target = "currentPlanVersion", ignore = true)
    @Mapping(target = "targetPlanVersion", ignore = true)
    @Mapping(target = "effectiveDate", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "appliedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SubscriptionChangeRequest toEntity(CreateSubscriptionChangeRequest request);

    SubscriptionChangeResponse toResponse(SubscriptionChangeRequest change);
}
