package com.sni.bokaticowork.features.notification.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.notification.dto.request.WebhookEndpointRequest;
import com.sni.bokaticowork.features.notification.dto.response.WebhookDeliveryResponse;
import com.sni.bokaticowork.features.notification.dto.response.WebhookEndpointResponse;
import com.sni.bokaticowork.features.notification.enums.NotificationDeliveryStatus;
import com.sni.bokaticowork.features.notification.service.interfaces.WebhookService;
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
@RequestMapping(ApiPath.V1)
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/webhook-endpoints")
    public ResponseEntity<WebhookEndpointResponse> create(@Valid @RequestBody WebhookEndpointRequest request) {
        return ResponseEntity.ok(webhookService.create(request));
    }

    @GetMapping("/webhook-endpoints")
    public ResponseEntity<List<WebhookEndpointResponse>> list(@RequestParam(required = false) Boolean active,
                                                              @RequestParam(required = false) String search) {
        return ResponseEntity.ok(webhookService.list(active, search));
    }

    @PatchMapping("/webhook-endpoints/{endpointCode}/activate")
    public ResponseEntity<WebhookEndpointResponse> activate(@PathVariable String endpointCode) {
        return ResponseEntity.ok(webhookService.setActive(endpointCode, true));
    }

    @PatchMapping("/webhook-endpoints/{endpointCode}/deactivate")
    public ResponseEntity<WebhookEndpointResponse> deactivate(@PathVariable String endpointCode) {
        return ResponseEntity.ok(webhookService.setActive(endpointCode, false));
    }

    @GetMapping("/webhook-deliveries")
    public ResponseEntity<Page<WebhookDeliveryResponse>> deliveries(@RequestParam(required = false) NotificationDeliveryStatus status,
                                                                    @RequestParam(required = false) String endpointCode,
                                                                    @RequestParam(required = false) String eventType,
                                                                    Pageable pageable) {
        return ResponseEntity.ok(webhookService.deliveries(status, endpointCode, eventType, pageable));
    }

    @PatchMapping("/webhook-deliveries/{deliveryNumber}/retry")
    public ResponseEntity<WebhookDeliveryResponse> retry(@PathVariable String deliveryNumber) {
        return ResponseEntity.ok(webhookService.retry(deliveryNumber));
    }
}
