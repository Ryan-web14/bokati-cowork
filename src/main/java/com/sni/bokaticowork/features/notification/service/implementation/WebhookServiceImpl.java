package com.sni.bokaticowork.features.notification.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.notification.dto.request.WebhookEndpointRequest;
import com.sni.bokaticowork.features.notification.dto.response.WebhookDeliveryResponse;
import com.sni.bokaticowork.features.notification.dto.response.WebhookEndpointResponse;
import com.sni.bokaticowork.features.notification.enums.NotificationDeliveryStatus;
import com.sni.bokaticowork.features.notification.mapper.interfaces.NotificationMapper;
import com.sni.bokaticowork.features.notification.model.WebhookDelivery;
import com.sni.bokaticowork.features.notification.model.WebhookEndpoint;
import com.sni.bokaticowork.features.notification.repository.WebhookDeliveryRepository;
import com.sni.bokaticowork.features.notification.repository.WebhookEndpointRepository;
import com.sni.bokaticowork.features.notification.service.interfaces.WebhookService;
import com.sni.bokaticowork.features.notification.service.support.WebhookSignatureSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class WebhookServiceImpl implements WebhookService {

    private static final String ENDPOINT_SEQUENCE = "webhook_endpoint";
    private static final String DELIVERY_SEQUENCE = "webhook_delivery";

    private final WebhookEndpointRepository endpointRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final NotificationMapper mapper;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final WebhookSignatureSupport signatureSupport;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public WebhookEndpointResponse create(WebhookEndpointRequest request) {
        validateEndpoint(request);
        WebhookEndpoint endpoint = mapper.toEntity(request);
        endpoint.setEndpointCode(sequenceGenerator.next(ENDPOINT_SEQUENCE));
        return mapper.toResponse(endpointRepository.save(endpoint));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WebhookEndpointResponse> list(Boolean active, String search) {
        return endpointRepository.search(active, normalize(search)).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public WebhookEndpointResponse setActive(String endpointCode, boolean active) {
        WebhookEndpoint endpoint = findEndpoint(endpointCode);
        endpoint.setActive(active);
        return mapper.toResponse(endpointRepository.save(endpoint));
    }

    @Override
    public int enqueueForEvent(String eventType, String aggregateType, String aggregateId, String payloadJson) {
        String normalizedEvent = upper(eventType);
        List<WebhookEndpoint> endpoints = endpointRepository.findActiveForEvent(normalizedEvent);
        for (WebhookEndpoint endpoint : endpoints) {
            WebhookDelivery delivery = WebhookDelivery.builder()
                    .deliveryNumber(sequenceGenerator.next(DELIVERY_SEQUENCE))
                    .endpoint(endpoint)
                    .eventType(normalizedEvent)
                    .aggregateType(upper(aggregateType))
                    .aggregateId(aggregateId)
                    .payloadJson(StringUtils.hasText(payloadJson) ? payloadJson : "{}")
                    .status(NotificationDeliveryStatus.PENDING)
                    .availableAt(Instant.now())
                    .build();
            deliveryRepository.save(delivery);
        }
        return endpoints.size();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WebhookDeliveryResponse> deliveries(NotificationDeliveryStatus status,
                                                    String endpointCode,
                                                    String eventType,
                                                    Pageable pageable) {
        return deliveryRepository.search(
                status == null ? null : status.name(),
                normalize(endpointCode),
                upper(eventType),
                pageable
        ).map(mapper::toResponse);
    }

    @Override
    public WebhookDeliveryResponse retry(String deliveryNumber) {
        WebhookDelivery delivery = findDelivery(deliveryNumber);
        delivery.setStatus(NotificationDeliveryStatus.PENDING);
        delivery.setAvailableAt(Instant.now());
        delivery.setLastError(null);
        return mapper.toResponse(deliveryRepository.save(delivery));
    }

    @Override
    public int processPending(int batchSize) {
        var deliveries = deliveryRepository.findByStatusInAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
                Set.of(NotificationDeliveryStatus.PENDING, NotificationDeliveryStatus.FAILED),
                Instant.now(),
                PageRequest.of(0, Math.max(batchSize, 1))
        );
        for (WebhookDelivery delivery : deliveries) {
            try {
                dispatch(delivery);
            } catch (RuntimeException ex) {
                fail(delivery, ex.getMessage());
            }
        }
        return deliveries.size();
    }

    private void dispatch(WebhookDelivery delivery) {
        delivery.setStatus(NotificationDeliveryStatus.PROCESSING);
        deliveryRepository.saveAndFlush(delivery);

        WebhookEndpoint endpoint = delivery.getEndpoint();
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint.getUrl()))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .header("X-Bokati-Event", delivery.getEventType())
                .header("X-Bokati-Delivery", delivery.getDeliveryNumber())
                .POST(HttpRequest.BodyPublishers.ofString(delivery.getPayloadJson()));

        String signature = signatureSupport.sign(delivery.getPayloadJson(), endpoint.getSecret());
        if (StringUtils.hasText(signature)) {
            request.header("X-Bokati-Signature", signature);
        }

        try {
            HttpResponse<String> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
            delivery.setHttpStatus(response.statusCode());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                delivery.setStatus(NotificationDeliveryStatus.DELIVERED);
                delivery.setDeliveredAt(Instant.now());
                delivery.setLastError(null);
            } else {
                fail(delivery, "Webhook returned HTTP " + response.statusCode());
                return;
            }
            deliveryRepository.save(delivery);
        } catch (Exception ex) {
            fail(delivery, ex.getMessage());
        }
    }

    private void fail(WebhookDelivery delivery, String error) {
        log.warn("Webhook delivery {} failed: {}", delivery.getDeliveryNumber(), error);
        delivery.setStatus(NotificationDeliveryStatus.FAILED);
        delivery.setAttempts(delivery.getAttempts() == null ? 1 : delivery.getAttempts() + 1);
        delivery.setLastError(error);
        delivery.setAvailableAt(Instant.now().plusSeconds(Math.min(3600, 60L * Math.max(1, delivery.getAttempts()))));
        deliveryRepository.save(delivery);
    }

    private WebhookEndpoint findEndpoint(String endpointCode) {
        if (!StringUtils.hasText(endpointCode)) {
            throw new BadRequestException("endpointCode is required");
        }
        return endpointRepository.findByEndpointCode(endpointCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Webhook endpoint " + endpointCode + " not found"));
    }

    private WebhookDelivery findDelivery(String deliveryNumber) {
        if (!StringUtils.hasText(deliveryNumber)) {
            throw new BadRequestException("deliveryNumber is required");
        }
        return deliveryRepository.findByDeliveryNumber(deliveryNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Webhook delivery " + deliveryNumber + " not found"));
    }

    private void validateEndpoint(WebhookEndpointRequest request) {
        if (!StringUtils.hasText(request.name())) {
            throw new BadRequestException("Webhook name is required");
        }
        if (!StringUtils.hasText(request.url())) {
            throw new BadRequestException("Webhook URL is required");
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String upper(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }
}
