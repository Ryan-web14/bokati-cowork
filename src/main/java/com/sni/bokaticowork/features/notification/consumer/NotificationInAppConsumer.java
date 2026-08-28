package com.sni.bokaticowork.features.notification.consumer;

import com.sni.bokaticowork.core.communication.mailService.dto.NotificationRabbitMessage;
import com.sni.bokaticowork.core.event.WebSocketTopics;
import com.sni.bokaticowork.core.event.dto.ClientNotificationPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationInAppConsumer {

    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = "notification.inapp", containerFactory = "notificationListenerFactory")
    public void consume(NotificationRabbitMessage message) {
        if (message.recipientEmail() == null) {
            log.warn("IN_APP notification {} missing recipientEmail, skipping",
                    message.notificationNumber());
            return;
        }

        ClientNotificationPayload payload = new ClientNotificationPayload(
                message.notificationNumber(),
                message.subject(),
                message.eventType(),
                message.aggregateType(),
                message.aggregateId(),
                message.payloadJson(),
                message.createdAt(),
                false
        );

        // The real-time WebSocket push is best-effort: the notification is already
        // persisted and the client fetches it on (re)connect. A broker outage
        // (STOMP relay not active) or an offline user must not send the message to the
        // DLQ, which would drop it permanently. Swallow delivery failures and ack.
        try {
            messagingTemplate.convertAndSendToUser(
                    message.recipientEmail(),
                    WebSocketTopics.USER_NOTIFICATIONS,
                    payload
            );

            messagingTemplate.convertAndSendToUser(
                    message.recipientEmail(),
                    WebSocketTopics.USER_UNREAD_COUNT,
                    Map.of("action", "INCREMENT")
            );

            log.debug("Pushed IN_APP notification {} to user {}",
                    message.notificationNumber(), message.recipientEmail());
        } catch (Exception ex) {
            log.warn("Real-time push of IN_APP notification {} to {} failed ({}) · notification stays persisted for retrieval on reconnect",
                    message.notificationNumber(), message.recipientEmail(), ex.getMessage());
        }
    }
}
