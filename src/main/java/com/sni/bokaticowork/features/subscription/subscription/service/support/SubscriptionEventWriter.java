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
                "{\"reason\":\"" + sanitize(reason) + "\",\"actor\":\"" + sanitize(actor) + "\"}"
        );
    }

    public void writeEvent(Subscription subscription, SubscriptionEventType eventType, String payload) {
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

    private String sanitize(String value) {
        return value == null ? "" : value.replace("\"", "'");
    }
}
