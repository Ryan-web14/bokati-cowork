package com.sni.bokaticowork.features.subscription.notification.service.impl;

import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.outbox.service.implementation.OutboxRecipientResolver;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.notification.dto.request.SendNotificationRequest;
import com.sni.bokaticowork.features.notification.enums.NotificationChannel;
import com.sni.bokaticowork.features.notification.enums.NotificationRecipientType;
import com.sni.bokaticowork.features.notification.service.support.NotificationDispatchSupport;
import com.sni.bokaticowork.features.notification.service.support.NotificationPayloadSupport;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SubscriptionNotificationServiceImpl implements SubscriptionNotificationService {

    private final SubscriptionNotificationOutboxRepository notificationRepository;
    private final SubscriptionNotificationMapper notificationMapper;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final SubscriptionTimelineService timelineService;
    private final @Lazy SubscriptionService subscriptionService;
    private final NotificationDispatchSupport notificationDispatch;
    private final NotificationPayloadSupport payloadSupport;
    private final OutboxRecipientResolver recipientResolver;

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

    /**
     * Remet chaque notification due au module de notification, qui l'envoie et la relance
     * (NotificationWorker). Une ligne ne passe DISPATCHED qu'une fois la remise acceptee ;
     * sans destinataire joignable ou sur un canal sans emetteur, elle passe FAILED avec la raison.
     * Une erreur technique pendant la remise fait echouer le lot entier : les lignes restent dues.
     */
    @Override
    public int dispatchDue(int limit) {
        List<SubscriptionNotificationOutbox> due = notificationRepository.findDueNotifications(Instant.now(), limit <= 0 ? 100 : limit);
        int dispatched = 0;
        for (SubscriptionNotificationOutbox notification : due) {
            notification.setAttemptCount(notification.getAttemptCount() + 1);
            String failure = handOver(notification);
            if (failure != null) {
                log.warn("Notification d'abonnement {} non envoyee : {}", notification.getNotificationNumber(), failure);
                notification.setStatus(SubscriptionNotificationStatus.FAILED);
                notification.setErrorMessage(failure);
                notificationRepository.save(notification);
                continue;
            }
            notification.setStatus(SubscriptionNotificationStatus.DISPATCHED);
            notification.setDispatchedAt(Instant.now());
            notification.setErrorMessage(null);
            notificationRepository.save(notification);
            timelineService.write(notification.getSubscription(), SubscriptionTimelineEventType.NOTIFICATION_DISPATCHED, "NOTIFICATION", notification.getNotificationNumber(), "Notification dispatched", notification.getNotificationType().name(), notification.getPayloadJson());
            dispatched++;
        }
        return dispatched;
    }

    /**
     * Cree le message du module de notification.
     *
     * @return la raison de l'echec, ou null si le message a ete accepte
     */
    private String handOver(SubscriptionNotificationOutbox notification) {
        NotificationChannel channel = channelOf(notification);
        if (channel == null) {
            return "Canal " + notification.getChannel() + " sans emetteur";
        }
        OutboxRecipientResolver.Recipient recipient = recipientOf(notification);
        if (recipient == null || !StringUtils.hasText(recipient.email())) {
            return "Aucune adresse courriel pour le destinataire " + notification.getRecipient();
        }
        Map<String, Object> payload;
        try {
            payload = payloadSupport.toMap(notification.getPayloadJson());
        } catch (RuntimeException ex) {
            return "Contenu illisible : " + ex.getMessage();
        }
        // Le gabarit generique affiche "message" : le corps s'il existe, sinon l'objet
        payload.putIfAbsent("message", StringUtils.hasText(notification.getBody()) ? notification.getBody() : notification.getSubject());
        payload.putIfAbsent("reference", notification.getNotificationNumber());
        if (notification.getSubscription() != null) {
            payload.putIfAbsent("subscriptionNumber", notification.getSubscription().getSubscriptionNumber());
        }
        notificationDispatch.create(new SendNotificationRequest(
                notification.getNotificationType().name(),
                "SUBSCRIPTION_NOTIFICATION",
                notification.getNotificationNumber(),
                channel,
                recipientTypeOf(notification.getOwnerType()),
                notification.getOwnerCode(),
                recipient.email().trim(),
                recipient.displayName(),
                notification.getSubject(),
                null,
                null,
                payload,
                null
        ));
        return null;
    }

    private NotificationChannel channelOf(SubscriptionNotificationOutbox notification) {
        if (notification.getChannel() == null) {
            return null;
        }
        return switch (notification.getChannel()) {
            case EMAIL -> NotificationChannel.EMAIL;
            case IN_APP -> NotificationChannel.IN_APP;
            case WEBHOOK -> NotificationChannel.WEBHOOK;
            case SMS, WHATSAPP -> null;
        };
    }

    /**
     * Le destinataire est une adresse, ou (cas des alertes de pass et de quota) le code du titulaire,
     * dont on lit l'adresse sur sa fiche.
     */
    private OutboxRecipientResolver.Recipient recipientOf(SubscriptionNotificationOutbox notification) {
        String recipient = notification.getRecipient();
        if (StringUtils.hasText(recipient) && recipient.contains("@")) {
            return new OutboxRecipientResolver.Recipient(null, recipient);
        }
        DocumentOwnerType ownerType = ownerTypeOf(notification.getOwnerType());
        String ownerCode = StringUtils.hasText(notification.getOwnerCode()) ? notification.getOwnerCode() : recipient;
        return ownerType == null ? null : recipientResolver.resolveByOwnerCode(ownerType, ownerCode);
    }

    private DocumentOwnerType ownerTypeOf(SubscriberType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case MEMBER -> DocumentOwnerType.MEMBER;
            case CUSTOMER -> DocumentOwnerType.CUSTOMER;
            case BUSINESS_ENTITY -> DocumentOwnerType.BUSINESS;
        };
    }

    private NotificationRecipientType recipientTypeOf(SubscriberType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case MEMBER -> NotificationRecipientType.MEMBER;
            case CUSTOMER -> NotificationRecipientType.CUSTOMER;
            case BUSINESS_ENTITY -> NotificationRecipientType.BUSINESS;
        };
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
