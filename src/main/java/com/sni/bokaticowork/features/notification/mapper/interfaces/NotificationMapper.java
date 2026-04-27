package com.sni.bokaticowork.features.notification.mapper.interfaces;

import com.sni.bokaticowork.features.notification.dto.request.NotificationTemplateRequest;
import com.sni.bokaticowork.features.notification.dto.request.SendNotificationRequest;
import com.sni.bokaticowork.features.notification.dto.request.WebhookEndpointRequest;
import com.sni.bokaticowork.features.notification.dto.response.NotificationMessageResponse;
import com.sni.bokaticowork.features.notification.dto.response.NotificationTemplateResponse;
import com.sni.bokaticowork.features.notification.dto.response.WebhookDeliveryResponse;
import com.sni.bokaticowork.features.notification.dto.response.WebhookEndpointResponse;
import com.sni.bokaticowork.features.notification.mapper.decorator.NotificationMapperDecorator;
import com.sni.bokaticowork.features.notification.model.NotificationMessage;
import com.sni.bokaticowork.features.notification.model.NotificationTemplate;
import com.sni.bokaticowork.features.notification.model.WebhookDelivery;
import com.sni.bokaticowork.features.notification.model.WebhookEndpoint;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(NotificationMapperDecorator.class)
public interface NotificationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "notificationNumber", ignore = true)
    @Mapping(target = "payloadJson", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "attempts", ignore = true)
    @Mapping(target = "sentAt", ignore = true)
    @Mapping(target = "lastError", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    NotificationMessage toEntity(SendNotificationRequest request);

    NotificationMessageResponse toResponse(NotificationMessage message);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    NotificationTemplate toEntity(NotificationTemplateRequest request);

    NotificationTemplateResponse toResponse(NotificationTemplate template);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "endpointCode", ignore = true)
    @Mapping(target = "eventTypes", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    WebhookEndpoint toEntity(WebhookEndpointRequest request);

    WebhookEndpointResponse toResponse(WebhookEndpoint endpoint);

    WebhookDeliveryResponse toResponse(WebhookDelivery delivery);

    default Set<String> map(String value) {
        if (!StringUtils.hasText(value)) {
            return Set.of("*");
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }
}
