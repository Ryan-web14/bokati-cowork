package com.sni.bokaticowork.features.subscription.subscription.mapper.interfaces;

import com.sni.bokaticowork.features.subscription.subscription.dto.response.PassResponse;
import com.sni.bokaticowork.features.subscription.subscription.mapper.decorator.SubscriptionPassMapperDecorator;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(SubscriptionPassMapperDecorator.class)
public interface SubscriptionPassMapper {

    default PassResponse toResponse(Pass pass) {
        return null;
    }
}
