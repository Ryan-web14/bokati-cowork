package com.sni.bokaticowork.core.communication.mailService.consumer;

import com.sni.bokaticowork.core.communication.mailService.dto.EmailRabbitMessage;
import com.sni.bokaticowork.core.communication.mailService.service.EmailDeliveryTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailDlqConsumer {

    private final EmailDeliveryTracker deliveryTracker;

    @RabbitListener(queues = "email.dlq", containerFactory = "dlqListenerFactory")
    public void consumeDlq(EmailRabbitMessage message) {
        log.error("[EMAIL DLQ] Livraison définitivement échouée · emailNumber={} to={} subject={}",
                message.emailNumber(), message.to(), message.subject());
        try {
            deliveryTracker.markFailed(
                    message.emailNumber(),
                    "Message transféré en DLQ après échec définitif de la livraison RabbitMQ"
            );
        } catch (Exception ex) {
            log.warn("[EMAIL DLQ] Impossible de mettre à jour le log de livraison pour {}: {}",
                    message.emailNumber(), ex.getMessage());
        }
    }
}
