package com.sni.bokaticowork.features.notification.service.implementation;

import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.notification.dto.request.NotificationTemplateRequest;
import com.sni.bokaticowork.features.notification.dto.request.PublishNotificationEventRequest;
import com.sni.bokaticowork.features.notification.dto.request.SendNotificationRequest;
import com.sni.bokaticowork.features.notification.dto.response.NotificationDispatchResponse;
import com.sni.bokaticowork.features.notification.dto.response.NotificationMessageResponse;
import com.sni.bokaticowork.features.notification.dto.response.NotificationTemplateResponse;
import com.sni.bokaticowork.features.notification.enums.NotificationChannel;
import com.sni.bokaticowork.features.notification.enums.NotificationDeliveryStatus;
import com.sni.bokaticowork.features.notification.mapper.interfaces.NotificationMapper;
import com.sni.bokaticowork.features.notification.repository.NotificationMessageRepository;
import com.sni.bokaticowork.features.notification.repository.NotificationTemplateRepository;
import com.sni.bokaticowork.features.notification.service.interfaces.NotificationService;
import com.sni.bokaticowork.features.notification.service.support.NotificationDispatchSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationDispatchSupport dispatchSupport;
    private final NotificationMessageRepository messageRepository;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationMapper mapper;
    private final ObjectProvider<OutboxService> outboxService;

    @Override
    public NotificationDispatchResponse send(SendNotificationRequest request) {
        return dispatchSupport.create(request);
    }

    @Override
    public void publish(PublishNotificationEventRequest request) {
        outboxService.getObject().publish(
                upper(request.eventType()),
                upper(request.aggregateType()),
                request.aggregateId(),
                request.payload() == null ? java.util.Map.of() : request.payload()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationMessageResponse> list(NotificationDeliveryStatus status,
                                                  NotificationChannel channel,
                                                  String eventType,
                                                  String recipientEmail,
                                                  String search,
                                                  Pageable pageable) {
        return messageRepository.search(
                status == null ? null : status.name(),
                channel == null ? null : channel.name(),
                upper(eventType),
                normalizeEmail(recipientEmail),
                normalize(search),
                pageable
        ).map(mapper::toResponse);
    }

    @Override
    public NotificationMessageResponse retry(String notificationNumber) {
        return mapper.toResponse(dispatchSupport.retry(notificationNumber));
    }

    @Override
    public NotificationTemplateResponse upsertTemplate(NotificationTemplateRequest request) {
        return templateRepository.findByTemplateCodeIgnoreCase(request.templateCode())
                .map(existing -> {
                    existing.setChannel(request.channel());
                    existing.setSubject(normalize(request.subject()));
                    existing.setTemplateName(normalize(request.templateName()));
                    existing.setBodyTemplate(request.bodyTemplate());
                    existing.setActive(request.active() == null || request.active());
                    existing.setAdminOnly(request.adminOnly() != null && request.adminOnly());
                    return mapper.toResponse(templateRepository.save(existing));
                })
                .orElseGet(() -> mapper.toResponse(templateRepository.save(mapper.toEntity(request))));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationTemplateResponse> listTemplates(NotificationChannel channel, Boolean active) {
        return templateRepository.list(channel == null ? null : channel.name(), active).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public int processPending(int batchSize) {
        return dispatchSupport.processPending(batchSize);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeEmail(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }

    private String upper(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }
}
