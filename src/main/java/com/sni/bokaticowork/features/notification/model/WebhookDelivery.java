package com.sni.bokaticowork.features.notification.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.notification.enums.NotificationDeliveryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "webhook_delivery", indexes = {
        @Index(name = "idx_webhook_delivery_number", columnList = "delivery_number"),
        @Index(name = "idx_webhook_delivery_status", columnList = "status,available_at"),
        @Index(name = "idx_webhook_delivery_event", columnList = "event_type,aggregate_type,aggregate_id")
})
public class WebhookDelivery {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "delivery_number", nullable = false, unique = true, length = 100)
    private String deliveryNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endpoint_id", nullable = false)
    private WebhookEndpoint endpoint;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Column(name = "aggregate_type", length = 80)
    private String aggregateType;

    @Column(name = "aggregate_id", length = 120)
    private String aggregateId;

    @org.hibernate.annotations.ColumnTransformer(write = "?::jsonb")
    @Column(name = "payload_json", columnDefinition = "jsonb")
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private NotificationDeliveryStatus status = NotificationDeliveryStatus.PENDING;

    @Column(name = "attempts", nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = NotificationDeliveryStatus.PENDING;
        }
        if (attempts == null) {
            attempts = 0;
        }
        if (availableAt == null) {
            availableAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
