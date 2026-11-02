package com.sni.bokaticowork.features.company.sharing.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.seat.model.SubscriptionSeat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Ce qu'une place recoit en propre sur un droit · remplace la part et le plafond de la regle.
 */
@Entity
@Table(name = "subscription_seat_allowance")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatAllowance {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private SubscriptionSeat seat;

    @Column(name = "entitlement_code", nullable = false, length = 100)
    private String entitlementCode;

    @Column(name = "quota", precision = 19, scale = 4)
    private BigDecimal quota;

    @Column(name = "cap", precision = 19, scale = 4)
    private BigDecimal cap;

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
