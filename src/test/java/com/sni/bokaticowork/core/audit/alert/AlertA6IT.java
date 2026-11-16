package com.sni.bokaticowork.core.audit.alert;

import com.sni.bokaticowork.core.communication.whatsapp.WhatsAppMessageSender;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.notification.service.interfaces.NotificationService;
import com.sni.bokaticowork.features.portal.notification.service.AdminInAppNotifier;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lot A6 sur une vraie base : une alerte s'ouvre au seuil, est envoyee une seule fois par les trois
 * canaux (un WhatsApp refuse n'empeche pas les autres), garde l'accuse de lecture, se ferme seule
 * quand la mesure redescend ; un seuil a 0 coupe la regle. La migration passe la validation du schema.
 */
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AlertA6IT extends PostgresIntegrationTestBase {

    @Autowired private JdbcTemplate jdbc;

    private final PlatformSettings settings = mock(PlatformSettings.class);
    private final AdminInAppNotifier inApp = mock(AdminInAppNotifier.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final WhatsAppMessageSender whatsapp = mock(WhatsAppMessageSender.class);

    @AfterEach
    void clean() {
        jdbc.update("DELETE FROM app_error WHERE path = '/a6-test'");
        jdbc.update("DELETE FROM audit_alert");
    }

    private AlertService service() {
        when(settings.number(anyString(), anyLong())).thenAnswer(invocation -> invocation.getArgument(1));
        when(settings.text(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        when(settings.text(eq(AlertDispatcher.EMAILS_KEY), anyString())).thenReturn("admin@example.com");
        when(settings.text(eq(AlertDispatcher.PHONES_KEY), anyString())).thenReturn("242055000000");
        when(whatsapp.send(anyString(), anyString(), any(), any(), anyString())).thenThrow(new BadRequestException("Aucun modele WhatsApp approuve"));
        AlertDispatcher dispatcher = new AlertDispatcher(settings, provider(inApp), provider(notifications), provider(whatsapp));
        return new AlertService(jdbc, new AlertMetrics(jdbc), dispatcher, settings);
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> provider(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }

    private void errors(int count) {
        for (int i = 0; i < count; i++) {
            jdbc.update("INSERT INTO app_error (path, status, exception_class) VALUES ('/a6-test', 500, 'java.lang.IllegalStateException')");
        }
    }

    @Test
    void anAlertOpensOnceIsSentByEveryChannelAndClosesByItself() {
        AlertService alerts = service();
        errors(10);
        alerts.evaluate();
        alerts.evaluate();

        List<AlertService.Alert> open = alerts.list("open", "SERVER_ERRORS", null, null, 0, 10).getContent();
        assertThat(open).singleElement().satisfies(alert -> {
            assertThat(alert.severity()).isEqualTo("CRITICAL");
            assertThat(alert.metricValue()).isEqualByComparingTo("10");
            assertThat(alert.notifications()).contains("APP ok").contains("EMAIL 1/1").contains("WHATSAPP 0/1");
        });
        verify(notifications, times(1)).send(any());
        verify(inApp, times(1)).broadcastAlert(eq("SERVER_ERRORS"), eq("AUDIT"), anyString(), anyString(), eq("CRITICAL"), anyString());

        AlertService.Alert acknowledged = alerts.acknowledge(open.getFirst().id(), "awa@example.com", "Je regarde");
        assertThat(acknowledged.status()).isEqualTo("ACKNOWLEDGED");
        assertThat(acknowledged.acknowledgedBy()).isEqualTo("awa@example.com");

        jdbc.update("DELETE FROM app_error WHERE path = '/a6-test'");
        alerts.evaluate();
        assertThat(alerts.get(open.getFirst().id()).status()).isEqualTo("RESOLVED");
        assertThat(alerts.get(open.getFirst().id()).resolvedAt()).isNotNull();
    }

    @Test
    void aZeroThresholdTurnsTheRuleOff() {
        AlertService alerts = service();
        when(settings.number(eq(AlertRule.SERVER_ERRORS.settingKey()), anyLong())).thenReturn(0L);
        errors(12);
        alerts.evaluate();
        assertThat(alerts.list(null, "SERVER_ERRORS", null, null, 0, 10).getContent()).isEmpty();
        assertThat(alerts.rules()).filteredOn(rule -> rule.code().equals("SERVER_ERRORS")).singleElement()
                .satisfies(rule -> assertThat(rule.enabled()).isFalse());
    }

    @Test
    void theDashboardGathersTheIndicators() {
        Map<String, Object> dashboard = service().dashboard();
        assertThat(dashboard).containsKeys("indicators", "trafficLastHour", "lockedAccounts", "failedLogins24h", "openAlerts");
        assertThat(((Map<?, ?>) dashboard.get("indicators")).keySet()).map(String::valueOf).contains("ERROR_RATE", "SLOW_RESPONSES", "TASK_FAILURES", "PAWAPAY_FAILURES");
        assertThat(AlertDispatcher.list("a@x.com, b@x.com;c@x.com a@x.com")).containsExactly("a@x.com", "b@x.com", "c@x.com");
    }
}
