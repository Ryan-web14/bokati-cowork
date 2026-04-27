package com.sni.bokaticowork.features.notification.repository;

import com.sni.bokaticowork.features.notification.model.WebhookEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface WebhookEndpointRepository extends JpaRepository<WebhookEndpoint, Long> {

    Optional<WebhookEndpoint> findByEndpointCode(String endpointCode);

    @Query(value = """
            SELECT *
            FROM webhook_endpoint endpoint
            WHERE endpoint.active = true
              AND (
                    endpoint.event_types IS NULL
                    OR endpoint.event_types = ''
                    OR endpoint.event_types ILIKE CONCAT('%', CAST(:eventType AS varchar), '%')
                    OR endpoint.event_types ILIKE '%*%'
              )
            ORDER BY endpoint.created_at ASC
            """, nativeQuery = true)
    List<WebhookEndpoint> findActiveForEvent(String eventType);

    @Query(value = """
            SELECT *
            FROM webhook_endpoint endpoint
            WHERE (CAST(:active AS boolean) IS NULL OR endpoint.active = CAST(:active AS boolean))
              AND (
                    CAST(:search AS varchar) IS NULL
                    OR endpoint.endpoint_code ILIKE CONCAT('%', CAST(:search AS varchar), '%')
                    OR endpoint.name ILIKE CONCAT('%', CAST(:search AS varchar), '%')
                    OR endpoint.url ILIKE CONCAT('%', CAST(:search AS varchar), '%')
              )
            ORDER BY endpoint.created_at DESC
            """, nativeQuery = true)
    List<WebhookEndpoint> search(Boolean active, String search);
}
