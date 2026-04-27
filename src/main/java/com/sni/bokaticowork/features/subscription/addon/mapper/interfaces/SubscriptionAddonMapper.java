package com.sni.bokaticowork.features.subscription.addon.mapper.interfaces;

import com.sni.bokaticowork.features.subscription.addon.dto.CreateSubscriptionAddonRequest;
import com.sni.bokaticowork.features.subscription.addon.dto.SubscriptionAddonResponse;
import com.sni.bokaticowork.features.subscription.addon.mapper.decorator.SubscriptionAddonMapperDecorator;
import com.sni.bokaticowork.features.subscription.addon.model.SubscriptionAddon;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(SubscriptionAddonMapperDecorator.class)
public interface SubscriptionAddonMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "subscription", ignore = true)
    @Mapping(target = "planVersion", ignore = true)
    @Mapping(target = "unitPrice", ignore = true)
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SubscriptionAddon toEntity(CreateSubscriptionAddonRequest request);

    SubscriptionAddonResponse toResponse(SubscriptionAddon addon);
}
