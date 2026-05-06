package com.sni.bokaticowork.features.subscription.notification.service.impl;

import com.sni.bokaticowork.features.subscription.notification.dto.CreateSubscriptionNotificationRequest;
import com.sni.bokaticowork.features.subscription.notification.enums.SubscriptionNotificationChannel;
import com.sni.bokaticowork.features.subscription.notification.service.SubscriptionNotificationService;
import com.sni.bokaticowork.features.subscription.subscription.service.support.entitlement.EntitlementQuotaThresholdEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class EntitlementQuotaAlertListener {

    private final SubscriptionNotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onQuotaThreshold(EntitlementQuotaThresholdEvent event) {
        String subject = event.threshold() == 100
                ? "Votre quota a été entièrement consommé"
                : "Vous avez consommé " + event.threshold() + "% de votre quota";

        String payload = buildPayload(event);

        try {
            notificationService.queue(new CreateSubscriptionNotificationRequest(
                    event.subscriptionNumber(),
                    event.ownerType(),
                    event.ownerCode(),
                    event.notificationType(),
                    SubscriptionNotificationChannel.EMAIL,
                    event.ownerCode(),
                    subject,
                    null,
                    payload,
                    Instant.now()
            ));
        } catch (Exception e) {
            log.warn("Failed to queue quota alert for grant {}: {}", event.grantNumber(), e.getMessage());
        }
    }

    private String buildPayload(EntitlementQuotaThresholdEvent event) {
        return String.format(
                "{\"grantNumber\":\"%s\",\"entitlementCode\":\"%s\",\"threshold\":%d,\"remaining\":%s,\"quantityGranted\":%s}",
                event.grantNumber(),
                event.entitlementCode(),
                event.threshold(),
                event.quantityRemaining(),
                event.quantityGranted()
        );
    }
}
