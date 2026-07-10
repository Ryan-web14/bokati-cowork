package com.sni.bokaticowork.features.notification.consumer;

import com.sni.bokaticowork.core.communication.mailService.dto.NotificationRabbitMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationDlqConsumer {

    @RabbitListener(queues = "notification.dlq", containerFactory = "dlqListenerFactory")
    public void consumeDlq(NotificationRabbitMessage message) {
        log.error("[NOTIFICATION DLQ] Livraison définitivement échouée · number={} channel={} to={} event={}",
                message.notificationNumber(), message.channel(), message.recipientEmail(), message.eventType());
    }

    @RabbitListener(queues = "notification.admin", containerFactory = "dlqListenerFactory")
    public void consumeAdmin(NotificationRabbitMessage message) {
        log.warn("[NOTIFICATION ADMIN] Alerte système reçue · number={} event={} aggregate={}/{}",
                message.notificationNumber(), message.eventType(),
                message.aggregateType(), message.aggregateId());
    }
}
