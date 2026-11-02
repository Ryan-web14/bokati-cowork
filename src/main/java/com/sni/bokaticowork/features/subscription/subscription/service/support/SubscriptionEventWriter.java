package com.sni.bokaticowork.features.subscription.subscription.service.support;

import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionEventType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.model.SubscriptionEvent;
import com.sni.bokaticowork.features.subscription.subscription.model.SubscriptionStatusHistory;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionEventRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionStatusHistoryRepository;
import com.sni.bokaticowork.features.subscription.timeline.enums.SubscriptionTimelineEventType;
import com.sni.bokaticowork.features.subscription.timeline.service.SubscriptionTimelineService;
import com.sni.bokaticowork.core.utils.json.JsonPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionEventWriter {

    private final SubscriptionStatusHistoryRepository historyRepository;
    private final SubscriptionEventRepository eventRepository;
    private final SubscriptionTimelineService timelineService;

    public void writeHistory(Subscription subscription, SubscriptionStatus from, SubscriptionStatus to, String reason, String actor) {
        historyRepository.save(SubscriptionStatusHistory.builder()
                .subscription(subscription)
                .fromStatus(from)
                .toStatus(to)
                .reason(reason)
                .changedBy(actor)
                .build());
        timelineService.write(
                subscription,
                SubscriptionTimelineEventType.STATUS_CHANGED,
                "SUBSCRIPTION_STATUS",
                subscription.getSubscriptionNumber(),
                "Subscription status changed",
                from + " -> " + to,
                JsonPayload.of("reason", reason == null ? "" : reason, "actor", actor == null ? "" : actor)
        );
    }

    public void writeEvent(Subscription subscription, SubscriptionEventType eventType, String rawPayload) {
        String payload = jsonPayload(rawPayload);
        eventRepository.save(SubscriptionEvent.builder()
                .subscription(subscription)
                .eventType(eventType)
                .payloadJson(payload)
                .build());
        timelineService.write(
                subscription,
                toTimelineEventType(eventType),
                "SUBSCRIPTION_EVENT",
                subscription.getSubscriptionNumber(),
                eventType.name(),
                null,
                payload
        );
    }

    private SubscriptionTimelineEventType toTimelineEventType(SubscriptionEventType eventType) {
        try {
            return SubscriptionTimelineEventType.valueOf(eventType.name());
        } catch (IllegalArgumentException ex) {
            return SubscriptionTimelineEventType.STATUS_CHANGED;
        }
    }

    /**
     * Le contenu de l'evenement · un JSON bien forme, produit par {@link JsonPayload}, ou rien.
     *
     * <p>Les colonnes sont en {@code jsonb} : un contenu mal forme y etait refuse au moment
     * d'ecrire, et l'operation entiere echouait (l'annulation d'un abonnement l'a montre). Il est
     * refuse ici, avec un message qui dit lequel, plutot que dans la base.</p>
     */
    String jsonPayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        if (!JsonPayload.isValid(payload)) {
            throw new IllegalArgumentException("Contenu d'evenement d'abonnement mal forme, a produire avec JsonPayload : "
                    + (payload.length() > 120 ? payload.substring(0, 120) + "..." : payload));
        }
        return payload;
    }
}
