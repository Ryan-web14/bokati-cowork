package com.sni.bokaticowork.features.notification.service.support;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.communication.mailService.dto.NotificationRabbitMessage;
import com.sni.bokaticowork.core.communication.mailService.enums.EmailPriority;
import com.sni.bokaticowork.core.communication.mailService.service.EmailRabbitPublisher;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.notification.dto.request.SendNotificationRequest;
import com.sni.bokaticowork.features.notification.dto.response.NotificationDispatchResponse;
import com.sni.bokaticowork.features.notification.enums.NotificationChannel;
import com.sni.bokaticowork.features.notification.enums.NotificationDeliveryStatus;
import com.sni.bokaticowork.features.notification.enums.NotificationRecipientType;
import com.sni.bokaticowork.features.notification.mapper.interfaces.NotificationMapper;
import com.sni.bokaticowork.features.notification.model.NotificationMessage;
import com.sni.bokaticowork.features.notification.repository.NotificationMessageRepository;
import com.sni.bokaticowork.features.notification.repository.NotificationTemplateRepository;
import com.sni.bokaticowork.features.notification.service.interfaces.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDispatchSupport {

    private static final String NOTIFICATION_SEQUENCE = "notification_message";

    private final NotificationMessageRepository messageRepository;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationMapper mapper;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final NotificationPayloadSupport payloadSupport;
    private final NotificationTemplateRenderer templateRenderer;
    private final WebhookService webhookService;
    private final EmailRabbitPublisher rabbitPublisher;
    private final DefaultEmailSender emailSender;

    @Transactional
    public NotificationDispatchResponse create(SendNotificationRequest request) {
        validate(request);
        NotificationMessage message = mapper.toEntity(request);
        message.setNotificationNumber(sequenceGenerator.next(NOTIFICATION_SEQUENCE));
        message.setPayloadJson(payloadSupport.toJson(enrichPayload(request.payload(), request.recipientName())));
        applyTemplate(message);
        NotificationMessage saved = messageRepository.save(message);
        int deliveries = webhookService.enqueueForEvent(
                saved.getEventType(),
                saved.getAggregateType(),
                saved.getAggregateId(),
                saved.getPayloadJson()
        );
        return new NotificationDispatchResponse(saved.getNotificationNumber(), deliveries);
    }

    @Transactional
    public NotificationDispatchResponse createForEvent(String eventType,
                                                       String aggregateType,
                                                       String aggregateId,
                                                       Map<String, Object> payload) {
        String recipientEmail = string(payload, "recipientEmail");
        String adminEmail = string(payload, "adminEmail");
        int webhookDeliveries = webhookService.enqueueForEvent(
                upper(eventType),
                upper(aggregateType),
                aggregateId,
                payloadSupport.toJson(payload)
        );

        String email = StringUtils.hasText(recipientEmail) ? recipientEmail : adminEmail;
        if (!StringUtils.hasText(email)) {
            return new NotificationDispatchResponse(null, webhookDeliveries);
        }

        SendNotificationRequest request = new SendNotificationRequest(
                eventType,
                aggregateType,
                aggregateId,
                NotificationChannel.EMAIL,
                recipientType(payload),
                string(payload, "recipientCode"),
                email,
                string(payload, "recipientName"),
                string(payload, "subject"),
                firstText(payload, "templateCode", "notificationTemplateCode", eventType),
                string(payload, "templateName"),
                payload,
                null
        );
        NotificationDispatchResponse created = create(request);
        return new NotificationDispatchResponse(created.notificationNumber(), webhookDeliveries);
    }

    @Transactional
    public int processPending(int batchSize) {
        var messages = messageRepository.findByStatusInAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
                Set.of(NotificationDeliveryStatus.PENDING, NotificationDeliveryStatus.FAILED),
                Instant.now(),
                PageRequest.of(0, Math.max(batchSize, 1))
        );

        for (NotificationMessage message : messages) {
            try {
                process(message);
            } catch (RuntimeException ex) {
                fail(message, ex.getMessage());
            }
        }
        return messages.size();
    }

    @Transactional
    public NotificationMessage retry(String notificationNumber) {
        NotificationMessage message = messageRepository.findByNotificationNumber(notificationNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Notification " + notificationNumber + " not found"));
        if (message.getStatus() == NotificationDeliveryStatus.SENT) {
            throw new BadRequestException("Notification " + notificationNumber + " was already sent on "
                    + message.getSentAt() + " and cannot be retried");
        }
        message.setStatus(NotificationDeliveryStatus.PENDING);
        message.setAvailableAt(Instant.now());
        message.setLastError(null);
        return messageRepository.save(message);
    }

    @Transactional
    public NotificationMessage dispatchIfDue(String notificationNumber) {
        NotificationMessage message = messageRepository.findByNotificationNumber(notificationNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Notification " + notificationNumber + " not found"));
        // Callers dispatch straight after create(), which races the worker for the same row.
        // Whoever gets there second must not send it again.
        if (message.getStatus() != NotificationDeliveryStatus.PENDING) {
            return message;
        }
        if (message.getAvailableAt() != null && message.getAvailableAt().isAfter(Instant.now())) {
            return message;
        }
        process(message);
        return message;
    }

    private void process(NotificationMessage message) {
        message.setStatus(NotificationDeliveryStatus.PROCESSING);
        messageRepository.saveAndFlush(message);

        if (message.getChannel() == NotificationChannel.IN_APP) {
            markSent(message);
            afterCommit(() -> publishInAppToRabbit(message));
            return;
        }
        if (message.getChannel() == NotificationChannel.WEBHOOK) {
            markSent(message);
            afterCommit(() -> publishWebhookToRabbit(message));
            return;
        }
        if (!StringUtils.hasText(message.getRecipientEmail())) {
            fail(message, "Recipient email is required for email notification");
            return;
        }

        // Render inside the transaction, so a template failure still marks the message FAILED, but
        // hand the mail over only once SENT is committed. Publishing inline meant a rollback of the
        // batch restored the row to PENDING with the mail already on the wire, and the next worker
        // run sent it a second time.
        Runnable publish = prepareEmailPublish(message);
        markSent(message);
        afterCommit(publish);
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private void publishInAppToRabbit(NotificationMessage message) {
        try {
            NotificationRabbitMessage rabbitMsg = new NotificationRabbitMessage(
                    message.getNotificationNumber(),
                    "IN_APP",
                    message.getRecipientEmail(),
                    message.getRecipientName(),
                    message.getEventType(),
                    message.getAggregateType(),
                    message.getAggregateId(),
                    message.getSubject(),
                    message.getPayloadJson(),
                    message.getTemplateCode(),
                    message.getTemplateName(),
                    message.getCreatedAt()
            );
            rabbitPublisher.publishNotification(rabbitMsg);
        } catch (Exception ex) {
            log.warn("Failed to publish IN_APP notification {} to RabbitMQ: {}",
                    message.getNotificationNumber(), ex.getMessage());
        }
    }

    private void publishWebhookToRabbit(NotificationMessage message) {
        try {
            NotificationRabbitMessage rabbitMsg = new NotificationRabbitMessage(
                    message.getNotificationNumber(),
                    "WEBHOOK",
                    message.getRecipientEmail(),
                    message.getRecipientName(),
                    message.getEventType(),
                    message.getAggregateType(),
                    message.getAggregateId(),
                    message.getSubject(),
                    message.getPayloadJson(),
                    message.getTemplateCode(),
                    message.getTemplateName(),
                    message.getCreatedAt()
            );
            rabbitPublisher.publishNotification(rabbitMsg);
        } catch (Exception ex) {
            log.warn("Failed to publish WEBHOOK notification {} to RabbitMQ: {}",
                    message.getNotificationNumber(), ex.getMessage());
        }
    }

    /**
     * Renders the mail now and returns the hand-off to run after commit. A rendering failure still
     * propagates inside the transaction, so the message is marked FAILED and retried as before.
     */
    private Runnable prepareEmailPublish(NotificationMessage message) {
        Map<String, Object> variables = payloadSupport.toMap(message.getPayloadJson());
        variables.putIfAbsent("recipientName", defaultText(message.getRecipientName(), "client"));
        variables.putIfAbsent("eventType", message.getEventType());
        variables.putIfAbsent("subject", message.getSubject());
        variables.putIfAbsent("aggregateId", message.getAggregateId());
        variables.putIfAbsent("aggregateType", message.getAggregateType());

        String html = renderBody(message, variables);
        EmailPriority priority = EmailPriority.fromEventType(message.getEventType());
        String recipient = message.getRecipientEmail();
        String subject = message.getSubject();
        String notificationNumber = message.getNotificationNumber();

        return () -> {
            try {
                emailSender.sendHtmlEmail(recipient, subject, html, priority);
            } catch (Exception ex) {
                // Past the commit the status can no longer be rolled back to FAILED. The send is
                // tracked on its own delivery log, which is where a failure surfaces from here.
                log.error("Failed to queue email for notification {} to {}: {}",
                        notificationNumber, recipient, ex.getMessage());
            }
        };
    }

    private void applyTemplate(NotificationMessage message) {
        if (!StringUtils.hasText(message.getTemplateCode())) {
            message.setTemplateCode(message.getEventType());
        }
        templateRepository.findByTemplateCodeIgnoreCaseAndChannelAndActiveTrue(message.getTemplateCode(), message.getChannel())
                .ifPresent(template -> applyTemplate(message, template));
        if (!StringUtils.hasText(message.getSubject())) {
            message.setSubject(defaultSubject(message.getEventType()));
        }
        if (!StringUtils.hasText(message.getTemplateName())) {
            message.setTemplateName("generic-notification");
        }
    }

    private void applyTemplate(NotificationMessage message,
                               com.sni.bokaticowork.features.notification.model.NotificationTemplate template) {
        if (StringUtils.hasText(template.getSubject())) {
            message.setSubject(template.getSubject());
        }
        if (StringUtils.hasText(template.getTemplateName())) {
            message.setTemplateName(template.getTemplateName());
        }
    }

    private String renderBody(NotificationMessage message, Map<String, Object> variables) {
        return templateRepository.findByTemplateCodeIgnoreCaseAndChannelAndActiveTrue(message.getTemplateCode(), message.getChannel())
                .filter(template -> StringUtils.hasText(template.getBodyTemplate()))
                .map(template -> templateRenderer.renderInline(template.getBodyTemplate(), variables))
                .orElseGet(() -> templateRenderer.render(message.getTemplateName(), variables));
    }

    private void markSent(NotificationMessage message) {
        message.setStatus(NotificationDeliveryStatus.SENT);
        message.setSentAt(Instant.now());
        message.setLastError(null);
        messageRepository.save(message);
    }

    private void fail(NotificationMessage message, String error) {
        log.warn("Notification {} failed: {}", message.getNotificationNumber(), error);
        message.setStatus(NotificationDeliveryStatus.FAILED);
        message.setAttempts(message.getAttempts() == null ? 1 : message.getAttempts() + 1);
        message.setLastError(error);
        message.setAvailableAt(Instant.now().plusSeconds(Math.min(3600, 60L * Math.max(1, message.getAttempts()))));
        messageRepository.save(message);
    }

    private void validate(SendNotificationRequest request) {
        if (!StringUtils.hasText(request.eventType())) {
            throw new BadRequestException("eventType is required");
        }
        if (request.channel() == null) {
            throw new BadRequestException("channel is required");
        }
        if (request.channel() == NotificationChannel.EMAIL && !StringUtils.hasText(request.recipientEmail())) {
            throw new BadRequestException("recipientEmail is required for email notifications");
        }
    }

    private Map<String, Object> enrichPayload(Map<String, Object> payload, String recipientName) {
        Map<String, Object> enriched = new LinkedHashMap<>();
        if (payload != null) {
            enriched.putAll(payload);
        }
        if (StringUtils.hasText(recipientName)) {
            enriched.putIfAbsent("recipientName", recipientName);
        }
        return enriched;
    }

    private NotificationRecipientType recipientType(Map<String, Object> payload) {
        String value = string(payload, "recipientType");
        if (!StringUtils.hasText(value)) {
            return NotificationRecipientType.ADMIN;
        }
        return NotificationRecipientType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    private String firstText(Map<String, Object> payload, String first, String second, String fallback) {
        String value = string(payload, first);
        if (StringUtils.hasText(value)) {
            return value;
        }
        value = string(payload, second);
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String string(Map<String, Object> payload, String key) {
        if (payload == null || payload.get(key) == null) {
            return null;
        }
        return payload.get(key).toString();
    }

    private String upper(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String defaultSubject(String eventType) {
        return "Notification " + defaultText(eventType, "Bokati");
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
