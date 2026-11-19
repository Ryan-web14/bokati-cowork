package com.sni.bokaticowork.features.booking.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Lot B3 (decision D3) : option « rendez-vous sans paiement, regle sur place » posee par l'equipe
 * pour un client (membre, client, entreprise). Elle l'emporte sur le reglage global : {@code
 * enabled = true} ouvre le portail a ce client, {@code false} le lui ferme. Retiree d'office apres
 * trop d'absences ({@code revokedAutomatically}).
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "booking_pay_on_site_grant")
public class BookingPayOnSiteGrant {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 30)
    private SubscriberType ownerType;

    @Column(name = "owner_code", nullable = false, length = 300)
    private String ownerCode;

    @Builder.Default
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = Boolean.TRUE;

    @Column(name = "reason", length = 500)
    private String reason;

    @Builder.Default
    @Column(name = "revoked_automatically", nullable = false)
    private Boolean revokedAutomatically = Boolean.FALSE;

    @Column(name = "updated_by", length = 180)
    private String updatedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void created() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
