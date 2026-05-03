package com.sni.bokaticowork.features.notification.controller;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.communication.mailService.dto.response.EmailDeliveryResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.notification.dto.request.NotificationTemplateRequest;
import com.sni.bokaticowork.features.notification.dto.request.PublishNotificationEventRequest;
import com.sni.bokaticowork.features.notification.dto.request.SendNotificationRequest;
import com.sni.bokaticowork.features.notification.dto.request.TestEmailRequest;
import com.sni.bokaticowork.features.notification.dto.request.TestNotificationRequest;
import com.sni.bokaticowork.features.notification.dto.response.NotificationDispatchResponse;
import com.sni.bokaticowork.features.notification.dto.response.NotificationMessageResponse;
import com.sni.bokaticowork.features.notification.dto.response.NotificationTemplateResponse;
import com.sni.bokaticowork.features.notification.enums.NotificationChannel;
import com.sni.bokaticowork.features.notification.enums.NotificationRecipientType;
import com.sni.bokaticowork.features.notification.enums.NotificationDeliveryStatus;
import com.sni.bokaticowork.features.notification.service.interfaces.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final DefaultEmailSender emailSender;

    @PostMapping
    public CompletableFuture<ResponseEntity<NotificationDispatchResponse>> send(@Valid @RequestBody SendNotificationRequest request) {
        return notificationService.send(request).thenApply(ResponseEntity::ok);
    }

    @PostMapping("/events")
    public ResponseEntity<Void> publish(@Valid @RequestBody PublishNotificationEventRequest request) {
        notificationService.publish(request);
        return ResponseEntity.accepted().build();
    }

    @GetMapping
    public ResponseEntity<Page<NotificationMessageResponse>> list(@RequestParam(required = false) NotificationDeliveryStatus status,
                                                                  @RequestParam(required = false) NotificationChannel channel,
                                                                  @RequestParam(required = false) String eventType,
                                                                  @RequestParam(required = false) String recipientEmail,
                                                                  @RequestParam(required = false) String search,
                                                                  Pageable pageable) {
        return ResponseEntity.ok(notificationService.list(status, channel, eventType, recipientEmail, search, pageable));
    }

    @PatchMapping("/{notificationNumber}/retry")
    public ResponseEntity<NotificationMessageResponse> retry(@PathVariable String notificationNumber) {
        return ResponseEntity.ok(notificationService.retry(notificationNumber));
    }

    @PostMapping("/templates")
    public ResponseEntity<NotificationTemplateResponse> upsertTemplate(@Valid @RequestBody NotificationTemplateRequest request) {
        return ResponseEntity.ok(notificationService.upsertTemplate(request));
    }

    @GetMapping("/templates")
    public ResponseEntity<List<NotificationTemplateResponse>> templates(@RequestParam(required = false) NotificationChannel channel,
                                                                        @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(notificationService.listTemplates(channel, active));
    }

    @PostMapping("/test/email")
    public ResponseEntity<EmailDeliveryResponse> testEmail(@Valid @RequestBody TestEmailRequest request) {
        String subject = org.springframework.util.StringUtils.hasText(request.subject())
                ? request.subject().trim()
                : "Test email Bokati";
        String content = org.springframework.util.StringUtils.hasText(request.content())
                ? request.content()
                : "Ceci est un email de test envoye par Bokati.";

        return ResponseEntity.accepted().body(emailSender.queueEmail(
                request.to(),
                subject,
                content,
                Boolean.TRUE.equals(request.html()),
                "NOTIFICATION",
                "TEST_EMAIL"
        ));
    }

    @PostMapping("/test/notification")
    public CompletableFuture<ResponseEntity<NotificationDispatchResponse>> testNotification(
            @Valid @RequestBody TestNotificationRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (request.payload() != null) {
            payload.putAll(request.payload());
        }
        payload.putIfAbsent("message", org.springframework.util.StringUtils.hasText(request.message())
                ? request.message()
                : "Notification de test Bokati");
        payload.putIfAbsent("recipientName", request.recipientName());
        payload.putIfAbsent("subject", request.subject());

        SendNotificationRequest notificationRequest = new SendNotificationRequest(
                "NOTIFICATION_TEST",
                "NOTIFICATION",
                "TEST",
                NotificationChannel.EMAIL,
                NotificationRecipientType.ADMIN,
                "TEST",
                request.to(),
                request.recipientName(),
                org.springframework.util.StringUtils.hasText(request.subject()) ? request.subject() : "Notification de test Bokati",
                "ADMIN_ALERT",
                "generic-notification",
                payload,
                null
        );
        return notificationService.send(notificationRequest).thenApply(ResponseEntity::ok);
    }
}
