package com.sni.bokaticowork.features.subscription.promotion.pricing.audience.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.promotion.model.Promotion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Une personne nommement designee par une promotion.
 *
 * <p>Le retrait ne supprime pas la ligne. Un geste accorde puis annule doit rester lisible : qui
 * l'avait accorde, pourquoi, et qui l'a retire. Une ligne effacee ne repond a aucune de ces
 * questions le jour ou le client rappelle.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "promotion_beneficiary", indexes = {
        @Index(name = "idx_promotion_beneficiary_subscriber", columnList = "subscriber_type,subscriber_code,revoked_at")
})
public class PromotionBeneficiary {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @Column(name = "subscriber_type", nullable = false, length = 60)
    private String subscriberType;

    @Column(name = "subscriber_code", nullable = false, length = 120)
    private String subscriberCode;

    @Column(name = "added_by", length = 120)
    private String addedBy;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    /** Pourquoi ce geste a ete accorde. Sans motif, un avantage nominatif est indefendable. */
    @Column(name = "added_reason", length = 255)
    private String addedReason;

    @Column(name = "notified_at")
    private Instant notifiedAt;

    @Column(name = "notification_channel", length = 60)
    private String notificationChannel;

    @Column(name = "redeemed_at")
    private Instant redeemedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_by", length = 120)
    private String revokedBy;

    @Column(name = "revoked_reason", length = 255)
    private String revokedReason;

    public boolean usable() {
        return revokedAt == null;
    }

    @PrePersist
    public void prePersist() {
        if (addedAt == null) {
            addedAt = Instant.now();
        }
    }
}
