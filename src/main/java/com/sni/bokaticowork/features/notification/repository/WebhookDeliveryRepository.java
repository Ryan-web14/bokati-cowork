package com.sni.bokaticowork.features.notification.repository;

import com.sni.bokaticowork.features.notification.enums.NotificationDeliveryStatus;
import com.sni.bokaticowork.features.notification.model.WebhookDelivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, Long> {

    Optional<WebhookDelivery> findByDeliveryNumber(String deliveryNumber);

    List<WebhookDelivery> findByStatusInAndAvailableAtLessThanEqualOrderByCreatedAtAsc(Set<NotificationDeliveryStatus> statuses,
                                                                                       Instant availableAt,
                                                                                       Pageable pageable);

    @Query(value = """
            SELECT delivery.*
            FROM webhook_delivery delivery
            JOIN webhook_endpoint endpoint ON endpoint.id = delivery.endpoint_id
            WHERE (CAST(:status AS varchar) IS NULL OR delivery.status = CAST(:status AS varchar))
              AND (CAST(:endpointCode AS varchar) IS NULL OR endpoint.endpoint_code = CAST(:endpointCode AS varchar))
              AND (CAST(:eventType AS varchar) IS NULL OR delivery.event_type = CAST(:eventType AS varchar))
            ORDER BY delivery.created_at DESC
            """,
            countQuery = """
            SELECT count(*)
            FROM webhook_delivery delivery
            JOIN webhook_endpoint endpoint ON endpoint.id = delivery.endpoint_id
            WHERE (CAST(:status AS varchar) IS NULL OR delivery.status = CAST(:status AS varchar))
              AND (CAST(:endpointCode AS varchar) IS NULL OR endpoint.endpoint_code = CAST(:endpointCode AS varchar))
              AND (CAST(:eventType AS varchar) IS NULL OR delivery.event_type = CAST(:eventType AS varchar))
            """,
            nativeQuery = true)
    Page<WebhookDelivery> search(String status, String endpointCode, String eventType, Pageable pageable);
}
