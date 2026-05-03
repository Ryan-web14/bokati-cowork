package com.sni.bokaticowork.features.subscription.subscription.service.support.subscription;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.PauseSubscriptionRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.SubscriptionStatusChangeRequest;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingScheduleStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionEventType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.repository.EntitlementGrantRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.EntitlementService;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionBillingSupport;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionEventWriter;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionPeriodCalculator;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionStatusManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SubscriptionLifecycleOperator {

    private final SubscriptionRepository subscriptionRepository;
    private final EntitlementGrantRepository entitlementGrantRepository;
    private final SubscriptionPeriodCalculator periodCalculator;
    private final SubscriptionBillingSupport billingSupport;
    private final SubscriptionStatusManager statusManager;
    private final SubscriptionEventWriter eventWriter;
    private final @Lazy EntitlementService entitlementService;
    private final SubscriptionEmailNotifier emailNotifier;

    public void activate(Subscription subscription, String reason, String actor) {
        if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
            return;
        }
        statusManager.changeStatus(subscription, SubscriptionStatus.ACTIVE, reason, actor);
        subscriptionRepository.save(subscription);
        // Use currently-valid ACTIVE grants only — expired/depleted/pass grants must not block re-activation
        if (entitlementGrantRepository.findCurrentlyActiveBySubscription(subscription.getId(), Instant.now()).isEmpty()) {
            entitlementService.grantForSubscription(subscription);
            eventWriter.writeEvent(subscription, SubscriptionEventType.ENTITLEMENTS_GRANTED, null);
        }
        eventWriter.writeEvent(subscription, SubscriptionEventType.SUBSCRIPTION_ACTIVATED, null);
        emailNotifier.notify(subscription, SubscriptionEventType.SUBSCRIPTION_ACTIVATED);
    }

    public Subscription suspend(Subscription subscription, SubscriptionStatusChangeRequest request) {
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE && subscription.getStatus() != SubscriptionStatus.PAST_DUE) {
            throw new ConflictException("subscription", "only ACTIVE or PAST_DUE subscriptions can be suspended");
        }
        statusManager.changeStatus(subscription, SubscriptionStatus.SUSPENDED, reason(request, "Suspension"), actor(request));
        subscription.setSuspendedAt(Instant.now());
        subscription.setSuspensionReason(reason(request, null));
        billingSupport.upsertBillingSchedule(subscription, BillingScheduleStatus.PAUSED);
        return subscriptionRepository.save(subscription);
    }

    public Subscription pause(Subscription subscription, PauseSubscriptionRequest request) {
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new ConflictException("subscription", "only ACTIVE subscriptions can be paused");
        }
        LocalDate pauseUntil = resolvePauseUntil(request);
        statusManager.changeStatus(subscription, SubscriptionStatus.PAUSED, pauseReason(request, "Subscription paused"), pauseActor(request));
        subscription.setPausedAt(Instant.now());
        subscription.setPauseUntil(pauseUntil);
        billingSupport.upsertBillingSchedule(subscription, BillingScheduleStatus.PAUSED);
        return subscriptionRepository.save(subscription);
    }

    public Subscription resume(Subscription subscription, SubscriptionStatusChangeRequest request) {
        if (subscription.getStatus() != SubscriptionStatus.PAUSED) {
            throw new ConflictException("subscription", "only PAUSED subscriptions can be resumed");
        }
        long pausedDays = subscription.getPauseUntil() == null
                ? 0
                : Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), subscription.getPauseUntil()));
        if (pausedDays > 0) {
            if (subscription.getCurrentPeriodEnd() != null) {
                subscription.setCurrentPeriodEnd(subscription.getCurrentPeriodEnd().plusDays(pausedDays));
            }
            if (subscription.getNextBillingDate() != null) {
                subscription.setNextBillingDate(subscription.getNextBillingDate().plusDays(pausedDays));
            }
        }
        subscription.setPauseUntil(null);
        statusManager.changeStatus(subscription, SubscriptionStatus.ACTIVE, reason(request, "Subscription resumed"), actor(request));
        billingSupport.upsertBillingSchedule(subscription, BillingScheduleStatus.ACTIVE);
        return subscriptionRepository.save(subscription);
    }

    public Subscription cancel(Subscription subscription, SubscriptionStatusChangeRequest request) {
        if (request != null && Boolean.TRUE.equals(request.cancelAtPeriodEnd())) {
            subscription.setCancelAtPeriodEnd(Boolean.TRUE);
            return subscriptionRepository.save(subscription);
        }
        statusManager.changeStatus(subscription, SubscriptionStatus.CANCELLED, reason(request, "Cancellation"), actor(request));
        subscription.setCancelledAt(Instant.now());
        subscription.setCancellationReason(reason(request, null));
        billingSupport.upsertBillingSchedule(subscription, BillingScheduleStatus.CANCELLED);
        Subscription saved = subscriptionRepository.save(subscription);
        emailNotifier.notify(saved, SubscriptionEventType.SUBSCRIPTION_CANCELLED);
        return saved;
    }

    public void renew(Subscription subscription) {
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new ConflictException("subscription", "only ACTIVE subscriptions can be renewed");
        }
        renewActive(subscription);
    }

    public int renewDueSubscriptions() {
        List<Subscription> subscriptions = subscriptionRepository.findAllByStatusAndNextBillingDateLessThanEqualAndAutoRenewTrue(
                SubscriptionStatus.ACTIVE.name(),
                LocalDate.now()
        );
        subscriptions.forEach(this::renewActive);
        return subscriptions.size();
    }

    public int cancelEndedSubscriptions() {
        List<Subscription> subscriptions = subscriptionRepository.findAllByCancelAtPeriodEndTrueAndCurrentPeriodEndLessThanEqualAndStatus(
                LocalDate.now(),
                SubscriptionStatus.ACTIVE.name()
        );
        subscriptions.forEach(subscription -> {
            statusManager.changeStatus(subscription, SubscriptionStatus.CANCELLED, "Cancellation at period end", "SYSTEM");
            subscription.setCancelledAt(Instant.now());
            Subscription saved = subscriptionRepository.save(subscription);
            emailNotifier.notify(saved, SubscriptionEventType.SUBSCRIPTION_CANCELLED);
        });
        return subscriptions.size();
    }

    public int repairActiveSubscriptionsWithoutGrants() {
        List<Subscription> subscriptions = subscriptionRepository.findActiveSubscriptionsWithoutGrants();
        subscriptions.forEach(subscription -> {
            entitlementService.grantForSubscription(subscription);
            eventWriter.writeEvent(subscription, SubscriptionEventType.ENTITLEMENTS_GRANTED, "{\"source\":\"INTEGRITY_WORKER\"}");
        });
        return subscriptions.size();
    }

    private void renewActive(Subscription subscription) {
        LocalDate newStart = subscription.getCurrentPeriodEnd() == null
                ? LocalDate.now()
                : subscription.getCurrentPeriodEnd().plusDays(1);
        subscription.setCurrentPeriodStart(newStart);
        subscription.setCurrentPeriodEnd(periodCalculator.periodEnd(newStart, subscription.getBillingCycle()));
        subscription.setNextBillingDate(periodCalculator.nextBillingDate(newStart, subscription.getBillingCycle()));
        billingSupport.createBillableItem(subscription, "SUBSCRIPTION_RENEWAL", "Subscription renewal");
        billingSupport.upsertBillingSchedule(subscription, BillingScheduleStatus.ACTIVE);
        subscriptionRepository.save(subscription);
        entitlementService.grantForSubscription(subscription);
        eventWriter.writeEvent(subscription, SubscriptionEventType.SUBSCRIPTION_RENEWED, null);
    }

    public String reason(SubscriptionStatusChangeRequest request, String defaultReason) {
        return request != null && StringUtils.hasText(request.reason()) ? request.reason().trim() : defaultReason;
    }

    public String actor(SubscriptionStatusChangeRequest request) {
        return request != null && StringUtils.hasText(request.changedBy()) ? request.changedBy().trim() : "SYSTEM";
    }

    private LocalDate resolvePauseUntil(PauseSubscriptionRequest request) {
        if (request != null && request.resumeDate() != null) {
            return request.resumeDate();
        }
        int days = request == null || request.days() == null ? 1 : request.days();
        return LocalDate.now().plusDays(Math.max(1, days));
    }

    private String pauseReason(PauseSubscriptionRequest request, String defaultReason) {
        return request != null && StringUtils.hasText(request.reason()) ? request.reason().trim() : defaultReason;
    }

    private String pauseActor(PauseSubscriptionRequest request) {
        return request != null && StringUtils.hasText(request.changedBy()) ? request.changedBy().trim() : "SYSTEM";
    }
}
