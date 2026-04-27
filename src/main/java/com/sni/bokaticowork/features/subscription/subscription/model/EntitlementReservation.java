package com.sni.bokaticowork.features.subscription.subscription.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementReservationStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
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

import java.math.BigDecimal;
import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "entitlement_reservation", indexes = {
        @Index(name = "idx_entitlement_reservation_reference", columnList = "reference_type,reference_id,status"),
        @Index(name = "idx_entitlement_reservation_owner", columnList = "owner_type,owner_code,status"),
        @Index(name = "idx_entitlement_reservation_expires", columnList = "expires_at,status")
})
public class EntitlementReservation {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_entitlement_reservation_grant"))
    private EntitlementGrant grant;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 60)
    private SubscriberType ownerType;

    @Column(name = "owner_code", nullable = false, length = 120)
    private String ownerCode;

    @Column(name = "entitlement_code", nullable = false, length = 100)
    private String entitlementCode;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "reference_type", nullable = false, length = 80)
    private String referenceType;

    @Column(name = "reference_id", nullable = false, length = 120)
    private String referenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private EntitlementReservationStatus status = EntitlementReservationStatus.ACTIVE;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

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
