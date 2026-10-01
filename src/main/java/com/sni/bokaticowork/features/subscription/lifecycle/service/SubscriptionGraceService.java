package com.sni.bokaticowork.features.subscription.lifecycle.service;

import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionPolicy;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionStatusHistoryRepository;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.SubscriptionStatusChangeRequest;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionEventType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.model.SubscriptionStatusHistory;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionEventWriter;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionStatusManager;
import com.sni.bokaticowork.features.subscription.subscription.service.support.subscription.SubscriptionLifecycleOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * La tolerance · entre l'echeance impayee et la suspension.
 *
 * <p>Une echeance impayee ne coupe pas les droits le jour meme. L'abonnement entre en
 * GRACE_PERIOD : il est prevenu, ses droits restent ouverts, et la suspension ne vient qu'au bout
 * du delai de la politique. Un paiement pendant la tolerance le rend ACTIVE sans intervention.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionGraceService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionStatusHistoryRepository historyRepository;
    private final SubscriptionPolicyService policyService;
    private final SubscriptionStatusManager statusManager;
    private final SubscriptionEventWriter eventWriter;
    private final OutboxService outboxService;
    private final @Lazy SubscriptionLifecycleOperator lifecycleOperator;

    public record Sweep(int entered, int suspended) {
    }

    @Transactional
    public boolean enter(Subscription subscription, String reason) {
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE && subscription.getStatus() != SubscriptionStatus.TRIALING
                && subscription.getStatus() != SubscriptionStatus.PAST_DUE) {
            return false;
        }
        SubscriptionPolicy policy = policyService.current();
        statusManager.changeStatus(subscription, SubscriptionStatus.GRACE_PERIOD, reason, "SYSTEM");
        subscriptionRepository.save(subscription);
        eventWriter.writeEvent(subscription, SubscriptionEventType.GRACE_PERIOD_ENTERED,
                "{\"reason\":\"" + reason.replace("\"", "'") + "\",\"suspensionInDays\":" + policy.getSuspensionAfterGraceDays() + "}");
        notify(subscription, "SUBSCRIPTION_GRACE_PERIOD", "Échéance impayée · votre abonnement " + subscription.getSubscriptionNumber(),
                Map.of("daysBeforeSuspension", String.valueOf(policy.getSuspensionAfterGraceDays())));
        log.info("Abonnement {} · tolerance ({}), suspension dans {} jours", subscription.getSubscriptionNumber(), reason,
                policy.getSuspensionAfterGraceDays());
        return true;
    }

    @Transactional
    public boolean exit(Subscription subscription, String reason) {
        if (subscription.getStatus() != SubscriptionStatus.GRACE_PERIOD) {
            return false;
        }
        statusManager.changeStatus(subscription, SubscriptionStatus.ACTIVE, reason, "SYSTEM");
        subscriptionRepository.save(subscription);
        eventWriter.writeEvent(subscription, SubscriptionEventType.GRACE_PERIOD_EXITED, null);
        log.info("Abonnement {} · sortie de tolerance ({})", subscription.getSubscriptionNumber(), reason);
        return true;
    }

    /**
     * Le balayage quotidien · qui entre en tolerance, qui en sort par suspension.
     *
     * <p>Entre : tout abonnement actif dont une echeance de renouvellement est impayee depuis plus
     * de {@code gracePeriodDays}. Suspendu : tout abonnement en tolerance depuis plus de
     * {@code suspensionAfterGraceDays}.</p>
     */
    @Transactional
    public Sweep sweep() {
        SubscriptionPolicy policy = policyService.current();
        int entered = 0;
        Instant unpaidSince = Instant.now().minus(policy.getGracePeriodDays(), ChronoUnit.DAYS);
        for (Subscription subscription : subscriptionRepository.findActiveWithRenewalUnpaidSince(unpaidSince)) {
            if (enter(subscription, "Échéance de renouvellement impayée depuis " + policy.getGracePeriodDays() + " jours")) {
                entered++;
            }
        }
        int suspended = 0;
        Instant suspendBefore = Instant.now().minus(policy.getSuspensionAfterGraceDays(), ChronoUnit.DAYS);
        for (Subscription subscription : subscriptionRepository.findAllByStatus(SubscriptionStatus.GRACE_PERIOD.name())) {
            Instant since = enteredAt(subscription);
            if (since != null && since.isBefore(suspendBefore)) {
                try {
                    lifecycleOperator.suspend(subscription, new SubscriptionStatusChangeRequest(
                            "Suspension · échéance impayée au terme de la tolérance de " + policy.getSuspensionAfterGraceDays() + " jours",
                            "SYSTEM", Boolean.FALSE));
                    suspended++;
                } catch (RuntimeException ex) {
                    log.warn("Abonnement {} · suspension apres tolerance impossible : {}", subscription.getSubscriptionNumber(), ex.getMessage());
                }
            }
        }
        return new Sweep(entered, suspended);
    }

    private Instant enteredAt(Subscription subscription) {
        List<SubscriptionStatusHistory> history = historyRepository.findAllBySubscriptionOrderByChangedAtAsc(subscription.getId());
        for (int i = history.size() - 1; i >= 0; i--) {
            if (history.get(i).getToStatus() == SubscriptionStatus.GRACE_PERIOD) {
                return history.get(i).getChangedAt();
            }
        }
        return subscription.getUpdatedAt();
    }

    private void notify(Subscription subscription, String eventType, String subject, Map<String, String> extra) {
        String email = recipientEmail(subscription);
        if (!StringUtils.hasText(email)) {
            return;
        }
        Map<String, Object> payload = new java.util.HashMap<>(extra);
        payload.put("recipientEmail", email);
        payload.put("subject", subject);
        payload.put("templateCode", eventType.toLowerCase());
        payload.put("subscriptionNumber", subscription.getSubscriptionNumber());
        outboxService.publish(eventType, "SUBSCRIPTION", subscription.getSubscriptionNumber(), payload);
    }

    public static String recipientEmail(Subscription subscription) {
        if (subscription.getMember() != null) return subscription.getMember().getEmail();
        if (subscription.getCustomer() != null) {
            return StringUtils.hasText(subscription.getCustomer().getBillingEmail())
                    ? subscription.getCustomer().getBillingEmail() : subscription.getCustomer().getEmail();
        }
        if (subscription.getBusinessEntity() != null) return subscription.getBusinessEntity().getEmail();
        return null;
    }
}
