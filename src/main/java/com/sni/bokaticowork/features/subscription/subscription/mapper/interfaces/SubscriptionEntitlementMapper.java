package com.sni.bokaticowork.features.subscription.subscription.mapper.interfaces;

import com.sni.bokaticowork.features.subscription.subscription.dto.response.EntitlementGrantResponse;
import com.sni.bokaticowork.features.subscription.subscription.mapper.decorator.SubscriptionEntitlementMapperDecorator;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementGrant;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(SubscriptionEntitlementMapperDecorator.class)
public interface SubscriptionEntitlementMapper {

    default EntitlementGrantResponse toResponse(EntitlementGrant grant) {
        return null;
    }
}
