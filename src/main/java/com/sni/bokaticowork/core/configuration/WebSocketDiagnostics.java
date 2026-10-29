package com.sni.bokaticowork.core.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.broker.BrokerAvailabilityEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Ce que fait le canal temps reel, dit la ou la production le lit.
 *
 * <p>Les navigateurs rappelaient {@code /ws/info} toutes les cinq secondes · le signe d'une
 * connexion qui s'ouvre puis se ferme aussitot. Rien ne disait pourquoi : Spring annonce l'etat du
 * relais vers CloudAMQP et les fermetures de session au niveau INFO de ses propres journaux, que la
 * production filtre. Ces trois evenements sont desormais ecrits sous notre nom :</p>
 * <ul>
 *   <li>le relais STOMP disponible ou perdu · s'il est perdu, chaque connexion est refusee ;</li>
 *   <li>une session ouverte (en DEBUG, c'est l'evenement courant) ;</li>
 *   <li>une session fermee autrement que normalement, avec le code et la raison.</li>
 * </ul>
 */
@Slf4j
@Component
public class WebSocketDiagnostics {

    private final AtomicInteger open = new AtomicInteger();

    @EventListener
    public void onBrokerAvailability(BrokerAvailabilityEvent event) {
        if (event.isBrokerAvailable()) {
            log.info("WebSocket · relais STOMP disponible");
        } else {
            log.warn("WebSocket · relais STOMP indisponible : les connexions du navigateur sont refusees "
                    + "jusqu'a son retour (verifier CloudAMQP, le port STOMP et le vhost)");
        }
    }

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        log.debug("WebSocket · session ouverte ({} ouverte(s))", open.incrementAndGet());
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        int remaining = Math.max(0, open.decrementAndGet());
        CloseStatus status = event.getCloseStatus();
        if (status == null || CloseStatus.NORMAL.equalsCode(status) || status.getCode() == CloseStatus.GOING_AWAY.getCode()) {
            log.debug("WebSocket · session fermee normalement ({} ouverte(s))", remaining);
            return;
        }
        log.info("WebSocket · session {} fermee · code {} {} ({} ouverte(s))", event.getSessionId(),
                status.getCode(), status.getReason() == null ? "" : status.getReason(), remaining);
    }
}
