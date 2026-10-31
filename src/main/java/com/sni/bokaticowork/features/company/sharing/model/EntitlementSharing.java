package com.sni.bokaticowork.features.company.sharing.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * La repartition d'un droit d'un abonnement d'organisation entre les places.
 */
@Entity
@Table(name = "subscription_entitlement_sharing")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntitlementSharing {

    public enum Mode {
        /** Pot commun · chaque place y puise jusqu'a epuisement, sous son plafond s'il y en a un. */
        POOL,
        /** Part nominative fixe par place · rien au-dela. */
        QUOTA,
        /** Sa part d'abord, puis le pot commun : ce qui n'est pas distribue en parts. */
        QUOTA_AND_POOL
    }

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "entitlement_code", nullable = false, length = 100)
    private String entitlementCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "sharing_mode", nullable = false, length = 30)
    private Mode mode;

    @Column(name = "default_quota", precision = 19, scale = 4)
    private BigDecimal defaultQuota;

    @Column(name = "seat_cap", precision = 19, scale = 4)
    private BigDecimal seatCap;

    @Column(name = "updated_by", length = 150)
    private String updatedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
