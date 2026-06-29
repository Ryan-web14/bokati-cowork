package com.sni.bokaticowork.features.notification.consumer;

import com.sni.bokaticowork.core.communication.mailService.dto.NotificationRabbitMessage;
import com.sni.bokaticowork.features.notification.service.interfaces.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWebhookConsumer {

    private final WebhookService webhookService;

    @RabbitListener(queues = "notification.webhook", containerFactory = "notificationListenerFactory")
    public void consume(NotificationRabbitMessage message) {
        webhookService.enqueueForEvent(
                message.eventType(),
                message.aggregateType(),
                message.aggregateId(),
                message.payloadJson()
        );
        log.debug("Webhook notification {} dispatched", message.notificationNumber());
    }
}
