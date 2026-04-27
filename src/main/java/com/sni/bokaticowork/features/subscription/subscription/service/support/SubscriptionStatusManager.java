package com.sni.bokaticowork.features.subscription.subscription.service.support;

import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionEventType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionStatusManager {

    private final SubscriptionItemRepository subscriptionItemRepository;
    private final SubscriptionEventWriter eventWriter;

    public void changeStatus(Subscription subscription, SubscriptionStatus newStatus, String reason, String actor) {
        SubscriptionStatus oldStatus = subscription.getStatus();
        subscription.setStatus(newStatus);
        subscriptionItemRepository.findAllBySubscription(subscription.getId()).forEach(item -> {
            item.setStatus(newStatus);
            subscriptionItemRepository.save(item);
        });
        eventWriter.writeHistory(subscription, oldStatus, newStatus, reason, actor);
        if (newStatus == SubscriptionStatus.CANCELLED) {
            eventWriter.writeEvent(subscription, SubscriptionEventType.SUBSCRIPTION_CANCELLED, null);
        } else if (newStatus == SubscriptionStatus.SUSPENDED) {
            eventWriter.writeEvent(subscription, SubscriptionEventType.SUBSCRIPTION_SUSPENDED, null);
        }
    }
}
