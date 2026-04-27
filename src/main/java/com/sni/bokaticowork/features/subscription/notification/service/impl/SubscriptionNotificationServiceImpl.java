package com.sni.bokaticowork.features.subscription.notification.service.impl;

import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.subscription.notification.dto.CreateSubscriptionNotificationRequest;
import com.sni.bokaticowork.features.subscription.notification.dto.SubscriptionNotificationResponse;
import com.sni.bokaticowork.features.subscription.notification.enums.SubscriptionNotificationStatus;
import com.sni.bokaticowork.features.subscription.notification.enums.SubscriptionNotificationType;
import com.sni.bokaticowork.features.subscription.notification.mapper.interfaces.SubscriptionNotificationMapper;
import com.sni.bokaticowork.features.subscription.notification.model.SubscriptionNotificationOutbox;
import com.sni.bokaticowork.features.subscription.notification.repository.SubscriptionNotificationOutboxRepository;
import com.sni.bokaticowork.features.subscription.notification.service.SubscriptionNotificationService;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.SubscriptionService;
import com.sni.bokaticowork.features.subscription.timeline.enums.SubscriptionTimelineEventType;
import com.sni.bokaticowork.features.subscription.timeline.service.SubscriptionTimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class SubscriptionNotificationServiceImpl implements SubscriptionNotificationService {

    private final SubscriptionNotificationOutboxRepository notificationRepository;
    private final SubscriptionNotificationMapper notificationMapper;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final SubscriptionTimelineService timelineService;
    private final @Lazy SubscriptionService subscriptionService;

    @Override
    public SubscriptionNotificationResponse queue(CreateSubscriptionNotificationRequest request) {
        Subscription subscription = StringUtils.hasText(request.subscriptionNumber())
                ? subscriptionService.getForService(request.subscriptionNumber())
                : null;
        SubscriptionNotificationOutbox notification = notificationMapper.toEntity(request);
        notification.setNotificationNumber(sequenceGenerator.next("subscription_notification"));
        notification.setSubscription(subscription);
        if (subscription != null) {
            notification.setOwnerType(subscription.getSubscriberType());
            notification.setOwnerCode(subscription.getSubscriberCode());
        }
        SubscriptionNotificationOutbox saved = notificationRepository.save(notification);
        timelineService.write(subscription, SubscriptionTimelineEventType.NOTIFICATION_QUEUED, "NOTIFICATION", saved.getNotificationNumber(), "Notification queued", saved.getNotificationType().name(), saved.getPayloadJson());
        return notificationMapper.toResponse(saved);
    }

    @Override
    public int dispatchDue(int limit) {
        List<SubscriptionNotificationOutbox> due = notificationRepository.findDueNotifications(Instant.now(), limit <= 0 ? 100 : limit);
        due.forEach(notification -> {
            notification.setStatus(SubscriptionNotificationStatus.DISPATCHED);
            notification.setDispatchedAt(Instant.now());
            notification.setAttemptCount(notification.getAttemptCount() + 1);
            notificationRepository.save(notification);
            timelineService.write(notification.getSubscription(), SubscriptionTimelineEventType.NOTIFICATION_DISPATCHED, "NOTIFICATION", notification.getNotificationNumber(), "Notification dispatched", notification.getNotificationType().name(), notification.getPayloadJson());
        });
        return due.size();
    }

    @Override
    public int cancel(String notificationNumber) {
        return notificationRepository.cancelPending(notificationNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<SubscriptionNotificationResponse> list(String subscriptionNumber,
                                                                    SubscriberType ownerType,
                                                                    String ownerCode,
                                                                    SubscriptionNotificationType notificationType,
                                                                    SubscriptionNotificationStatus status,
                                                                    Pageable pageable) {
        return new PaginatedResponse<>(notificationRepository.search(
                trim(subscriptionNumber),
                ownerType == null ? null : ownerType.name(),
                trim(ownerCode),
                notificationType == null ? null : notificationType.name(),
                status == null ? null : status.name(),
                pageable
        ).map(notificationMapper::toResponse));
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
