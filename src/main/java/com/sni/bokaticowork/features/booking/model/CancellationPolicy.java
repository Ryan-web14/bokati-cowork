package com.sni.bokaticowork.features.booking.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
@Table(name = "cancellation_policy")
public class CancellationPolicy {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "hours_before_start", nullable = false)
    private Integer hoursBeforeStart;

    @Column(name = "refund_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal refundPercentage;

    @Column(name = "rule_order", nullable = false)
    private Integer ruleOrder;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

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
