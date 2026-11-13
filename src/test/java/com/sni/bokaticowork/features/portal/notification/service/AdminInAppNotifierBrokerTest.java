package com.sni.bokaticowork.features.portal.notification.service;

import com.sni.bokaticowork.core.event.WebSocketTopics;
import com.sni.bokaticowork.core.event.dto.AdminAlertEvent;
import com.sni.bokaticowork.features.notification.service.interfaces.NotificationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.broker.BrokerAvailabilityEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Production du 13/11/2026 : une alerte emise une seconde apres le demarrage, avant la connexion
 * au courtier de messages, etait perdue. Elle attend maintenant la connexion.
 */
class AdminInAppNotifierBrokerTest {

    @Test
    void shouldKeepAnAlertUntilTheBrokerIsAvailable() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        AdminInAppNotifier notifier = new AdminInAppNotifier(mock(NotificationService.class), template);
        doThrow(new MessageDeliveryException("Message broker not active"))
                .doNothing()
                .when(template).convertAndSend(eq(WebSocketTopics.ADMIN_ALERTS), any(Object.class));

        notifier.broadcastAlert("BILLING_QUOTE_EXPIRED", "BILLING", "2 devis expirés", "2 devis", "INFO", null);
        notifier.onBrokerAvailability(new BrokerAvailabilityEvent(true, this));
        notifier.onBrokerAvailability(new BrokerAvailabilityEvent(true, this));

        ArgumentCaptor<Object> sent = ArgumentCaptor.forClass(Object.class);
        verify(template, times(2)).convertAndSend(eq(WebSocketTopics.ADMIN_ALERTS), sent.capture());
        assertThat(sent.getAllValues().get(1)).isInstanceOfSatisfying(AdminAlertEvent.class,
                alert -> assertThat(alert.alertType()).isEqualTo("BILLING_QUOTE_EXPIRED"));
    }
}
