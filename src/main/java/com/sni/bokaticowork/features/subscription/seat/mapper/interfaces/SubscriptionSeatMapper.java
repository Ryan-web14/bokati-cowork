package com.sni.bokaticowork.features.subscription.seat.mapper.interfaces;

import com.sni.bokaticowork.features.subscription.seat.dto.CreateSubscriptionSeatRequest;
import com.sni.bokaticowork.features.subscription.seat.dto.SubscriptionSeatResponse;
import com.sni.bokaticowork.features.subscription.seat.mapper.decorator.SubscriptionSeatMapperDecorator;
import com.sni.bokaticowork.features.subscription.seat.model.SubscriptionSeat;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(SubscriptionSeatMapperDecorator.class)
public interface SubscriptionSeatMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "subscription", ignore = true)
    @Mapping(target = "member", ignore = true)
    @Mapping(target = "memberCode", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "invitedAt", ignore = true)
    @Mapping(target = "activatedAt", ignore = true)
    @Mapping(target = "removedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SubscriptionSeat toEntity(CreateSubscriptionSeatRequest request);

    SubscriptionSeatResponse toResponse(SubscriptionSeat seat);
}
