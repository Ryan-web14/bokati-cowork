package com.sni.bokaticowork.features.notification.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
@Table(name = "webhook_endpoint", indexes = {
        @Index(name = "idx_webhook_endpoint_code", columnList = "endpoint_code"),
        @Index(name = "idx_webhook_endpoint_active", columnList = "active")
})
public class WebhookEndpoint {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "endpoint_code", nullable = false, unique = true, length = 100)
    private String endpointCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "url", nullable = false, columnDefinition = "text")
    private String url;

    @Column(name = "secret", columnDefinition = "text")
    private String secret;

    @Column(name = "event_types", columnDefinition = "text")
    private String eventTypes;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (active == null) {
            active = true;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
