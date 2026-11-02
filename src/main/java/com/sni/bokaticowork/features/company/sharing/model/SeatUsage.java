package com.sni.bokaticowork.features.company.sharing.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Ce qu'une place a pris sur un droit pour une reservation · sur sa part, sur le pot.
 */
@Entity
@Table(name = "subscription_seat_usage")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatUsage {

    public enum Status { RESERVED, CONSUMED, RELEASED }

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "seat_id", nullable = false)
    private Long seatId;

    @Column(name = "subscription_id", nullable = false)
    private Long subscriptionId;

    @Column(name = "member_code", nullable = false, length = 120)
    private String memberCode;

    @Column(name = "entitlement_code", nullable = false, length = 100)
    private String entitlementCode;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "reference_type", nullable = false, length = 80)
    private String referenceType;

    @Column(name = "reference_id", nullable = false, length = 120)
    private String referenceId;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "from_quota", nullable = false, precision = 19, scale = 4)
    private BigDecimal fromQuota;

    @Column(name = "from_pool", nullable = false, precision = 19, scale = 4)
    private BigDecimal fromPool;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

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
