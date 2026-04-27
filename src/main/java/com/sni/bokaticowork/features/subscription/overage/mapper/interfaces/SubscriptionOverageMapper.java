package com.sni.bokaticowork.features.subscription.overage.mapper.interfaces;

import com.sni.bokaticowork.features.subscription.overage.dto.CreateOveragePolicyRequest;
import com.sni.bokaticowork.features.subscription.overage.dto.OverageChargeResponse;
import com.sni.bokaticowork.features.subscription.overage.dto.OveragePolicyResponse;
import com.sni.bokaticowork.features.subscription.overage.mapper.decorator.SubscriptionOverageMapperDecorator;
import com.sni.bokaticowork.features.subscription.overage.model.SubscriptionOverageCharge;
import com.sni.bokaticowork.features.subscription.overage.model.SubscriptionOveragePolicy;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(SubscriptionOverageMapperDecorator.class)
public interface SubscriptionOverageMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "planVersion", ignore = true)
    @Mapping(target = "entitlementDefinition", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SubscriptionOveragePolicy toEntity(CreateOveragePolicyRequest request);

    OveragePolicyResponse toResponse(SubscriptionOveragePolicy policy);

    OverageChargeResponse toResponse(SubscriptionOverageCharge charge);
}
