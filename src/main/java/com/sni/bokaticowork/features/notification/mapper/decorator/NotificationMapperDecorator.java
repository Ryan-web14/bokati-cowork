package com.sni.bokaticowork.features.notification.mapper.decorator;

import com.sni.bokaticowork.features.notification.dto.request.NotificationTemplateRequest;
import com.sni.bokaticowork.features.notification.dto.request.SendNotificationRequest;
import com.sni.bokaticowork.features.notification.dto.request.WebhookEndpointRequest;
import com.sni.bokaticowork.features.notification.dto.response.NotificationMessageResponse;
import com.sni.bokaticowork.features.notification.dto.response.NotificationTemplateResponse;
import com.sni.bokaticowork.features.notification.dto.response.WebhookDeliveryResponse;
import com.sni.bokaticowork.features.notification.dto.response.WebhookEndpointResponse;
import com.sni.bokaticowork.features.notification.mapper.interfaces.NotificationMapper;
import com.sni.bokaticowork.features.notification.model.NotificationMessage;
import com.sni.bokaticowork.features.notification.model.NotificationTemplate;
import com.sni.bokaticowork.features.notification.model.WebhookDelivery;
import com.sni.bokaticowork.features.notification.model.WebhookEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public abstract class NotificationMapperDecorator implements NotificationMapper {

    @Autowired
    @Qualifier("delegate")
    private NotificationMapper delegate;

    @Override
    public NotificationMessage toEntity(SendNotificationRequest request) {
        NotificationMessage message = delegate.toEntity(request);
        message.setEventType(upper(request.eventType()));
        message.setAggregateType(upper(request.aggregateType()));
        message.setAggregateId(trim(request.aggregateId()));
        message.setRecipientCode(trim(request.recipientCode()));
        message.setRecipientEmail(lower(request.recipientEmail()));
        message.setRecipientName(trim(request.recipientName()));
        message.setSubject(trim(request.subject()));
        message.setTemplateCode(upper(request.templateCode()));
        message.setTemplateName(trim(request.templateName()));
        message.setAvailableAt(request.availableAt());
        return message;
    }

    @Override
    public NotificationMessageResponse toResponse(NotificationMessage message) {
        return new NotificationMessageResponse(
                message.getNotificationNumber(),
                message.getEventType(),
                message.getAggregateType(),
                message.getAggregateId(),
                message.getChannel(),
                message.getRecipientType(),
                message.getRecipientCode(),
                message.getRecipientEmail(),
                message.getRecipientName(),
                message.getSubject(),
                message.getTemplateCode(),
                message.getTemplateName(),
                message.getPayloadJson(),
                message.getStatus(),
                message.getAttempts(),
                message.getAvailableAt(),
                message.getSentAt(),
                message.getLastError(),
                message.getCreatedAt()
        );
    }

    @Override
    public NotificationTemplate toEntity(NotificationTemplateRequest request) {
        NotificationTemplate template = delegate.toEntity(request);
        template.setTemplateCode(upper(request.templateCode()));
        template.setSubject(trim(request.subject()));
        template.setTemplateName(trim(request.templateName()));
        template.setBodyTemplate(request.bodyTemplate());
        template.setActive(request.active() == null || request.active());
        template.setAdminOnly(request.adminOnly() != null && request.adminOnly());
        return template;
    }

    @Override
    public NotificationTemplateResponse toResponse(NotificationTemplate template) {
        return new NotificationTemplateResponse(
                template.getTemplateCode(),
                template.getChannel(),
                template.getSubject(),
                template.getTemplateName(),
                template.getBodyTemplate(),
                template.getActive(),
                template.getAdminOnly(),
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }

    @Override
    public WebhookEndpoint toEntity(WebhookEndpointRequest request) {
        WebhookEndpoint endpoint = delegate.toEntity(request);
        endpoint.setName(trim(request.name()));
        endpoint.setUrl(trim(request.url()));
        endpoint.setSecret(trim(request.secret()));
        endpoint.setEventTypes(joinEventTypes(request.eventTypes()));
        endpoint.setActive(request.active() == null || request.active());
        return endpoint;
    }

    @Override
    public WebhookEndpointResponse toResponse(WebhookEndpoint endpoint) {
        return new WebhookEndpointResponse(
                endpoint.getEndpointCode(),
                endpoint.getName(),
                endpoint.getUrl(),
                splitEventTypes(endpoint.getEventTypes()),
                endpoint.getActive(),
                endpoint.getCreatedAt(),
                endpoint.getUpdatedAt()
        );
    }

    @Override
    public WebhookDeliveryResponse toResponse(WebhookDelivery delivery) {
        return new WebhookDeliveryResponse(
                delivery.getDeliveryNumber(),
                delivery.getEndpoint().getEndpointCode(),
                delivery.getEndpoint().getName(),
                delivery.getEventType(),
                delivery.getAggregateType(),
                delivery.getAggregateId(),
                delivery.getPayloadJson(),
                delivery.getStatus(),
                delivery.getAttempts(),
                delivery.getAvailableAt(),
                delivery.getDeliveredAt(),
                delivery.getHttpStatus(),
                delivery.getLastError(),
                delivery.getCreatedAt()
        );
    }

    private String joinEventTypes(Set<String> eventTypes) {
        if (eventTypes == null || eventTypes.isEmpty()) {
            return "*";
        }
        return eventTypes.stream()
                .filter(StringUtils::hasText)
                .map(this::upper)
                .collect(Collectors.joining(","));
    }

    private Set<String> splitEventTypes(String value) {
        if (!StringUtils.hasText(value)) {
            return Set.of("*");
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private String upper(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String lower(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
