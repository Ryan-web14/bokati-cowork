package com.sni.bokaticowork.features.subscription.notification.mapper.interfaces;

import com.sni.bokaticowork.features.subscription.notification.dto.CreateSubscriptionNotificationRequest;
import com.sni.bokaticowork.features.subscription.notification.dto.SubscriptionNotificationResponse;
import com.sni.bokaticowork.features.subscription.notification.mapper.decorator.SubscriptionNotificationMapperDecorator;
import com.sni.bokaticowork.features.subscription.notification.model.SubscriptionNotificationOutbox;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(SubscriptionNotificationMapperDecorator.class)
public interface SubscriptionNotificationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "notificationNumber", ignore = true)
    @Mapping(target = "subscription", ignore = true)
    @Mapping(target = "dispatchedAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "attemptCount", ignore = true)
    @Mapping(target = "errorMessage", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SubscriptionNotificationOutbox toEntity(CreateSubscriptionNotificationRequest request);

    SubscriptionNotificationResponse toResponse(SubscriptionNotificationOutbox notification);
}
