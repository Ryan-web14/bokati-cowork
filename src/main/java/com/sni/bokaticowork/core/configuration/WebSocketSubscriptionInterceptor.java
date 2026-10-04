package com.sni.bokaticowork.core.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * Un abonnement se verifie par sa destination, pas seulement par la presence d'un compte.
 *
 * <p>Le seul controle etait d'etre authentifie, et uniquement pour {@code /topic/}. N'importe quel
 * membre du portail client pouvait donc s'abonner a {@code /topic/admin/alerts} et recevoir en
 * direct le flux de paiement nominatif de toute la maison. Les files {@code /queue/} n'exigeaient
 * meme pas d'identite.</p>
 *
 * <p>Desormais toute destination demande une identite, et les diffusions internes demandent un
 * role de la maison · voir {@link WebSocketDestinations}.</p>
 */
@Slf4j
@Component
public class WebSocketSubscriptionInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.SUBSCRIBE) {
            return message;
        }

        String destination = accessor.getDestination();
        Principal principal = accessor.getUser();
        Authentication user = principal instanceof Authentication authentication ? authentication : null;

        if (!WebSocketDestinations.permits(user, destination)) {
            if (user == null) {
                log.warn("Abonnement refuse a {} · aucune identite", destination);
            } else {
                // Nommer le compte · un abonne qui tente une destination interne est soit une
                // interface mal configuree, soit quelqu'un qui cherche. Les deux se diagnostiquent.
                log.warn("Abonnement refuse a {} · {} n'appartient pas au personnel", destination, user.getName());
            }
            throw new MessagingException("Subscription to " + destination + " is not permitted");
        }

        return message;
    }
}
