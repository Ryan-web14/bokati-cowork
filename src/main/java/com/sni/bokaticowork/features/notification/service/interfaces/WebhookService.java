package com.sni.bokaticowork.features.notification.service.interfaces;

import com.sni.bokaticowork.features.notification.dto.request.WebhookEndpointRequest;
import com.sni.bokaticowork.features.notification.dto.response.WebhookDeliveryResponse;
import com.sni.bokaticowork.features.notification.dto.response.WebhookEndpointResponse;
import com.sni.bokaticowork.features.notification.enums.NotificationDeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface WebhookService {

    WebhookEndpointResponse create(WebhookEndpointRequest request);

    List<WebhookEndpointResponse> list(Boolean active, String search);

    WebhookEndpointResponse setActive(String endpointCode, boolean active);

    int enqueueForEvent(String eventType, String aggregateType, String aggregateId, String payloadJson);

    Page<WebhookDeliveryResponse> deliveries(NotificationDeliveryStatus status,
                                             String endpointCode,
                                             String eventType,
                                             Pageable pageable);

    WebhookDeliveryResponse retry(String deliveryNumber);

    int processPending(int batchSize);
}
