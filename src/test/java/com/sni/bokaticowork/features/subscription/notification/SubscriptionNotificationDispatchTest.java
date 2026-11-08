package com.sni.bokaticowork.features.subscription.notification;

import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.outbox.service.implementation.OutboxRecipientResolver;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.notification.dto.request.SendNotificationRequest;
import com.sni.bokaticowork.features.notification.dto.response.NotificationDispatchResponse;
import com.sni.bokaticowork.features.notification.enums.NotificationChannel;
import com.sni.bokaticowork.features.notification.service.support.NotificationDispatchSupport;
import com.sni.bokaticowork.features.notification.service.support.NotificationPayloadSupport;
import com.sni.bokaticowork.features.subscription.notification.enums.SubscriptionNotificationChannel;
import com.sni.bokaticowork.features.subscription.notification.enums.SubscriptionNotificationStatus;
import com.sni.bokaticowork.features.subscription.notification.enums.SubscriptionNotificationType;
import com.sni.bokaticowork.features.subscription.notification.mapper.interfaces.SubscriptionNotificationMapper;
import com.sni.bokaticowork.features.subscription.notification.model.SubscriptionNotificationOutbox;
import com.sni.bokaticowork.features.subscription.notification.repository.SubscriptionNotificationOutboxRepository;
import com.sni.bokaticowork.features.subscription.notification.service.impl.SubscriptionNotificationServiceImpl;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.SubscriptionService;
import com.sni.bokaticowork.features.subscription.timeline.service.SubscriptionTimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Une alerte d'abonnement due est remise au module de notification avant de passer DISPATCHED ;
 * sans adresse joignable elle passe FAILED et rien n'est envoye.
 */
class SubscriptionNotificationDispatchTest {

    private SubscriptionNotificationOutboxRepository repository;
    private NotificationDispatchSupport dispatch;
    private OutboxRecipientResolver recipients;
    private SubscriptionNotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(SubscriptionNotificationOutboxRepository.class);
        dispatch = mock(NotificationDispatchSupport.class);
        recipients = mock(OutboxRecipientResolver.class);
        when(dispatch.create(any())).thenReturn(new NotificationDispatchResponse("NOT-1", 0));
        service = new SubscriptionNotificationServiceImpl(
                repository,
                mock(SubscriptionNotificationMapper.class),
                mock(SequenceGeneratorFacade.class),
                mock(SubscriptionTimelineService.class),
                mock(SubscriptionService.class),
                dispatch,
                new NotificationPayloadSupport(new ObjectMapper()),
                recipients
        );
    }

    private SubscriptionNotificationOutbox passAlert() {
        // Les alertes de pass et de quota mettent le code du titulaire comme destinataire
        return SubscriptionNotificationOutbox.builder()
                .notificationNumber("SNO-1")
                .ownerType(SubscriberType.MEMBER)
                .ownerCode("MBR-1")
                .notificationType(SubscriptionNotificationType.PASS_LOW_BALANCE)
                .channel(SubscriptionNotificationChannel.EMAIL)
                .recipient("MBR-1")
                .subject("Il vous reste 2 passages")
                .payloadJson("{\"passNumber\":\"PASS-1\",\"remainingUses\":\"2\"}")
                .scheduledAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Alerte due : courriel remis au module de notification, puis DISPATCHED")
    void dueAlertIsHandedOverThenDispatched() {
        SubscriptionNotificationOutbox alert = passAlert();
        when(repository.findDueNotifications(any(), anyInt())).thenReturn(List.of(alert));
        when(recipients.resolveByOwnerCode(DocumentOwnerType.MEMBER, "MBR-1"))
                .thenReturn(new OutboxRecipientResolver.Recipient("Awa", "awa@example.com"));

        int dispatched = service.dispatchDue(10);

        ArgumentCaptor<SendNotificationRequest> sent = ArgumentCaptor.forClass(SendNotificationRequest.class);
        verify(dispatch).create(sent.capture());
        assertThat(sent.getValue().channel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(sent.getValue().recipientEmail()).isEqualTo("awa@example.com");
        assertThat(sent.getValue().subject()).isEqualTo("Il vous reste 2 passages");
        assertThat(sent.getValue().payload()).containsEntry("passNumber", "PASS-1");
        assertThat(alert.getStatus()).isEqualTo(SubscriptionNotificationStatus.DISPATCHED);
        assertThat(dispatched).isEqualTo(1);
    }

    @Test
    @DisplayName("Sans adresse pour le titulaire : FAILED avec la raison, rien n'est envoye")
    void noAddressFails() {
        SubscriptionNotificationOutbox alert = passAlert();
        when(repository.findDueNotifications(any(), anyInt())).thenReturn(List.of(alert));

        int dispatched = service.dispatchDue(10);

        verify(dispatch, never()).create(any());
        assertThat(alert.getStatus()).isEqualTo(SubscriptionNotificationStatus.FAILED);
        assertThat(alert.getErrorMessage()).contains("MBR-1");
        assertThat(alert.getAttemptCount()).isEqualTo(1);
        assertThat(dispatched).isZero();
    }
}
