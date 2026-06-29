package com.sni.bokaticowork.features.subscription.subscription.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "pass_plan_price", indexes = {
        @Index(name = "idx_pass_plan_price_version", columnList = "pass_plan_version_id")
})
public class PassPlanPrice {

    @Id
    @IdGeneration
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pass_plan_version_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_pass_plan_price_version"))
    private PassPlanVersion passVersion;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "setup_fee", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal setupFee = BigDecimal.ZERO;

    @Column(name = "deposit_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Column(name = "tax_included", nullable = false)
    @Builder.Default
    private Boolean taxIncluded = Boolean.TRUE;

    @Column(name = "tax_code", length = 80)
    private String taxCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() { createdAt = updatedAt = Instant.now(); }

    @PreUpdate
    public void preUpdate() { updatedAt = Instant.now(); }
}
