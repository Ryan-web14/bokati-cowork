package com.sni.bokaticowork.core.communication.mailService.service;

import com.sni.bokaticowork.core.communication.mailService.dto.EmailRabbitMessage;
import com.sni.bokaticowork.core.communication.mailService.dto.NotificationRabbitMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailRabbitPublisher {

    private static final String EMAIL_EXCHANGE = "email";
    private static final String NOTIFICATION_EXCHANGE = "notification";

    private final RabbitTemplate rabbitTemplate;

    /**
     * Depose le courriel dans la file, et dit si c'est fait.
     *
     * <p>Un broker injoignable n'est pas une erreur de l'operation qui envoie : une inscription ne
     * doit pas echouer parce que la file des courriels est tombee. Le courriel est deja enregistre
     * en base, statut QUEUED ; {@code EmailRequeueWorker} le redeposera quand le broker sera la.
     * L'appelant qui a besoin de savoir regarde le retour, les autres continuent.</p>
     *
     * @return {@code false} si le broker n'a pas pris le message · il reste a redeposer
     */
    public boolean publishEmail(EmailRabbitMessage message) {
        try {
            rabbitTemplate.convertAndSend(EMAIL_EXCHANGE, message.routingKey(), message);
            log.debug("Published email {} to {} with priority {}",
                    message.emailNumber(), message.to(), message.priority());
            return true;
        } catch (AmqpException ex) {
            log.warn("Courriel {} pour {} non depose · broker injoignable ({}) · il sera redepose",
                    message.emailNumber(), message.to(), ex.getMessage());
            return false;
        }
    }

    public void publishNotification(NotificationRabbitMessage message) {
        rabbitTemplate.convertAndSend(NOTIFICATION_EXCHANGE, message.routingKey(), message);
        log.debug("Published notification {} to channel {}",
                message.notificationNumber(), message.channel());
    }
}
