package com.sni.bokaticowork.features.subscription.subscription.service.support.entitlement;

import com.sni.bokaticowork.features.subscription.notification.enums.SubscriptionNotificationType;
import com.sni.bokaticowork.features.subscription.repository.EntitlementGrantRepository;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementGrant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class EntitlementQuotaAlertService {

    private static final BigDecimal THRESHOLD_80  = BigDecimal.valueOf(80);
    private static final BigDecimal THRESHOLD_100 = BigDecimal.valueOf(100);

    private final ApplicationEventPublisher eventPublisher;
    private final EntitlementGrantRepository grantRepository;

    public void checkAndAlert(EntitlementGrant grant, BigDecimal afterQuantity) {
        if (Boolean.TRUE.equals(grant.getUnlimited())) return;
        if (grant.getQuantityGranted() == null || grant.getQuantityGranted().signum() == 0) return;

        BigDecimal consumed = grant.getQuantityGranted().subtract(afterQuantity);
        BigDecimal pctUsed = consumed.divide(grant.getQuantityGranted(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        if (pctUsed.compareTo(THRESHOLD_100) >= 0 && grant.getAlert100SentAt() == null) {
            publish(grant, 100, afterQuantity, SubscriptionNotificationType.ENTITLEMENT_EXHAUSTED);
            grant.setAlert100SentAt(Instant.now());
            grantRepository.save(grant);
        } else if (pctUsed.compareTo(THRESHOLD_80) >= 0 && grant.getAlert80SentAt() == null) {
            publish(grant, 80, afterQuantity, SubscriptionNotificationType.ENTITLEMENT_LOW_BALANCE);
            grant.setAlert80SentAt(Instant.now());
            grantRepository.save(grant);
        }
    }

    private void publish(EntitlementGrant grant, int threshold,
                         BigDecimal remaining, SubscriptionNotificationType type) {
        String subscriptionNumber = grant.getSubscription() != null
                ? grant.getSubscription().getSubscriptionNumber()
                : null;
        String entitlementCode = grant.getEntitlementDefinition() != null
                ? grant.getEntitlementDefinition().getCode()
                : "";

        eventPublisher.publishEvent(new EntitlementQuotaThresholdEvent(
                grant.getGrantNumber(),
                subscriptionNumber,
                grant.getOwnerType(),
                grant.getOwnerCode(),
                entitlementCode,
                grant.getQuantityGranted(),
                remaining,
                threshold,
                type
        ));
    }
}
