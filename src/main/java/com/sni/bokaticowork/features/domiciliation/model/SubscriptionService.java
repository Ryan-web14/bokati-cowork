package com.sni.bokaticowork.features.domiciliation.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Un service souscrit · l'abonnement devient un contenant.
 *
 * <p>Il porte un plan, et un ou plusieurs services. Le service a son propre cycle de vie : il peut
 * attendre un contrat, etre suspendu, se terminer sans que l'abonnement ne bouge.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "subscription_service")
public class SubscriptionService {

    public enum Status {
        /** Souscrit, mais pas encore rendu · un contrat ou une ressource manque. */
        PENDING,
        ACTIVE,
        SUSPENDED,
        TERMINATED
    }

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "service_number", nullable = false, unique = true, length = 100)
    private String serviceNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_definition_id", nullable = false)
    private ServiceDefinition serviceDefinition;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "unit_price", precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "XAF";

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "suspended_at")
    private Instant suspendedAt;

    @Column(name = "terminated_at")
    private Instant terminatedAt;

    @Column(name = "service_metadata_json", columnDefinition = "text")
    private String serviceMetadataJson;

    @Column(name = "created_by", length = 120)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
