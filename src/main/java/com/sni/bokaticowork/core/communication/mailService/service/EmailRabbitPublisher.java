package com.sni.bokaticowork.core.communication.mailService.service;

import com.sni.bokaticowork.core.communication.mailService.dto.EmailRabbitMessage;
import com.sni.bokaticowork.core.communication.mailService.dto.NotificationRabbitMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailRabbitPublisher {

    private static final String EMAIL_EXCHANGE = "email";
    private static final String NOTIFICATION_EXCHANGE = "notification";

    private final RabbitTemplate rabbitTemplate;

    public void publishEmail(EmailRabbitMessage message) {
        rabbitTemplate.convertAndSend(EMAIL_EXCHANGE, message.routingKey(), message);
        log.debug("Published email {} to {} with priority {}",
                message.emailNumber(), message.to(), message.priority());
    }

    public void publishNotification(NotificationRabbitMessage message) {
        rabbitTemplate.convertAndSend(NOTIFICATION_EXCHANGE, message.routingKey(), message);
        log.debug("Published notification {} to channel {}",
                message.notificationNumber(), message.channel());
    }
}
