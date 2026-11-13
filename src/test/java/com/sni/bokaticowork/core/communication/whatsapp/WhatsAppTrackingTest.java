package com.sni.bokaticowork.core.communication.whatsapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.communication.whatsapp.infobip.InfobipWhatsAppClient;
import com.sni.bokaticowork.core.communication.whatsapp.infobip.InfobipWhatsAppProperties;
import com.sni.bokaticowork.features.notification.enums.NotificationChannel;
import com.sni.bokaticowork.features.notification.enums.NotificationDeliveryStatus;
import com.sni.bokaticowork.features.notification.model.NotificationMessage;
import com.sni.bokaticowork.features.notification.repository.NotificationMessageRepository;
import com.sni.bokaticowork.features.notification.service.support.WhatsAppNotificationTracker;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Suivi des messages WhatsApp : signature du webhook, rapports d'Infobip, reflet dans notification_message. */
class WhatsAppTrackingTest {

    private static final String SECRET = "un-secret-de-webhook-assez-long-123";
    private static final String DELIVERED = "{\"results\":[{\"messageId\":\"m1\",\"to\":\"242055645569\",\"sentAt\":\"2026-11-13T01:57:13.516+0000\","
            + "\"doneAt\":\"2026-11-13T01:57:17.000+0000\",\"status\":{\"groupName\":\"DELIVERED\",\"name\":\"DELIVERED_TO_HANDSET\","
            + "\"description\":\"Message delivered to handset\"},\"error\":{\"groupName\":\"OK\",\"name\":\"NO_ERROR\"}}]}";

    @Test
    void signatureIsCheckedOnTheRawBody() throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(DELIVERED.getBytes(StandardCharsets.UTF_8));
        assertThat(WhatsAppWebhookController.verify(SECRET, "HMAC_SHA_256", DELIVERED, HexFormat.of().formatHex(digest))).isTrue();
        assertThat(WhatsAppWebhookController.verify(SECRET, "HMAC_SHA_256", DELIVERED, "sha256=" + HexFormat.of().formatHex(digest))).isTrue();
        assertThat(WhatsAppWebhookController.verify(SECRET, "HMAC_SHA_256", DELIVERED, Base64.getEncoder().encodeToString(digest))).isTrue();
        assertThat(WhatsAppWebhookController.verify(SECRET, "HMAC_SHA_256", DELIVERED + " ", HexFormat.of().formatHex(digest))).isFalse();
        assertThat(WhatsAppWebhookController.verify(null, "HMAC_SHA_256", DELIVERED, HexFormat.of().formatHex(digest))).isFalse();
        assertThat(WhatsAppWebhookController.verify(SECRET, "HMAC_SHA_256", DELIVERED, null)).isFalse();
    }

    @Test
    void reportsAreReadFromInfobipFormat() throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> item = (Map<String, Object>) ((java.util.List<?>) new ObjectMapper().readValue(DELIVERED, Map.class).get("results")).get(0);
        InfobipWhatsAppClient.Report report = InfobipWhatsAppClient.report(item);
        assertThat(report.statusGroup()).isEqualTo("DELIVERED");
        assertThat(report.error()).isNull();
        assertThat(report.doneAt()).isEqualTo(Instant.parse("2026-11-13T01:57:17Z"));
        assertThat(InfobipWhatsAppClient.instant("2026-11-13T01:57:17Z")).isEqualTo(Instant.parse("2026-11-13T01:57:17Z"));
        assertThat(InfobipWhatsAppClient.report(Map.of("messageId", "m2", "status", Map.of("groupName", "UNDELIVERABLE"),
                "error", Map.of("groupName", "HANDSET_ERRORS", "name", "EC_ABSENT_SUBSCRIBER", "description", "Absent"))).error()).contains("EC_ABSENT_SUBSCRIBER");
    }

    @Test
    void webhookUpdatesOnlySignedCalls() throws Exception {
        InfobipWhatsAppProperties properties = new InfobipWhatsAppProperties();
        properties.setWebhookSecret(SECRET);
        WhatsAppService service = mock(WhatsAppService.class);
        WhatsAppWebhookController controller = new WhatsAppWebhookController(properties, service, new ObjectMapper());
        assertThat(controller.receive(DELIVERED, "faux").getStatusCode().value()).isEqualTo(401);
        verify(service, never()).applyReport(any());
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of().formatHex(mac.doFinal(DELIVERED.getBytes(StandardCharsets.UTF_8)));
        assertThat(controller.receive(DELIVERED, signature).getStatusCode().value()).isEqualTo(200);
        verify(service).applyReport(any());
        String seen = "{\"results\":[{\"messageId\":\"m1\",\"seenAt\":\"2026-11-13T02:00:00.000+0000\"}]}";
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        controller.receive(seen, HexFormat.of().formatHex(mac.doFinal(seen.getBytes(StandardCharsets.UTF_8))));
        verify(service).applySeen("m1", Instant.parse("2026-11-13T02:00:00Z"));
    }

    @Test
    void notificationFollowsTheMessage() {
        NotificationMessageRepository repository = mock(NotificationMessageRepository.class);
        WhatsAppNotificationTracker tracker = new WhatsAppNotificationTracker(repository);
        when(repository.findByNotificationNumber("WA-m1")).thenReturn(Optional.empty());
        tracker.onStatus(new WhatsAppStatusEvent("m1", "WA-m1", "242055645569", "BOOKING_CONFIRMED", "reservation_confirmee", "BKG-1",
                "PENDING", "PENDING_ENROUTE", null, null, Instant.now(), null, null));
        ArgumentCaptor<NotificationMessage> saved = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(repository).save(saved.capture());
        NotificationMessage message = saved.getValue();
        assertThat(message.getChannel()).isEqualTo(NotificationChannel.WHATSAPP);
        assertThat(message.getStatus()).isEqualTo(NotificationDeliveryStatus.SENT);
        assertThat(message.getRecipientPhone()).isEqualTo("242055645569");

        when(repository.findByNotificationNumber("WA-m1")).thenReturn(Optional.of(message));
        Instant seenAt = Instant.parse("2026-11-13T02:00:00Z");
        tracker.onStatus(new WhatsAppStatusEvent("m1", "WA-m1", "242055645569", "BOOKING_CONFIRMED", "reservation_confirmee", "BKG-1",
                "DELIVERED", "DELIVERED_TO_HANDSET", null, null, null, Instant.now(), seenAt));
        assertThat(message.getStatus()).isEqualTo(NotificationDeliveryStatus.DELIVERED);
        assertThat(message.getReadAt()).isEqualTo(seenAt);

        tracker.onStatus(new WhatsAppStatusEvent("m1", "WA-m1", "242055645569", null, null, "BKG-1", "UNDELIVERABLE", "UNDELIVERABLE_NOT_DELIVERED",
                "Absent", "EC_ABSENT_SUBSCRIBER : Absent", null, Instant.now(), null));
        assertThat(message.getStatus()).isEqualTo(NotificationDeliveryStatus.FAILED);
        assertThat(message.getLastError()).contains("EC_ABSENT_SUBSCRIBER");
    }
}
