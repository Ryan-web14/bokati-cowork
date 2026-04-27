package com.sni.bokaticowork.features.notification.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.notification.dto.request.NotificationTemplateRequest;
import com.sni.bokaticowork.features.notification.dto.request.PublishNotificationEventRequest;
import com.sni.bokaticowork.features.notification.dto.request.SendNotificationRequest;
import com.sni.bokaticowork.features.notification.dto.response.NotificationDispatchResponse;
import com.sni.bokaticowork.features.notification.dto.response.NotificationMessageResponse;
import com.sni.bokaticowork.features.notification.dto.response.NotificationTemplateResponse;
import com.sni.bokaticowork.features.notification.enums.NotificationChannel;
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

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<NotificationDispatchResponse> send(@Valid @RequestBody SendNotificationRequest request) {
        return ResponseEntity.ok(notificationService.send(request));
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
}
