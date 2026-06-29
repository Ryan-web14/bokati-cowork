package com.sni.bokaticowork.features.ressource.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;
import com.sni.bokaticowork.features.ressource.enums.ResourcePriceAdjustmentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;


@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "resource_pricing_rule")
public class ResourcePricingRule {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false, foreignKey = @ForeignKey(name = "resource_pricing_rule_resource_fk"))
    private Resource resource;

    @Column(name = "unit", nullable = false)
    @Enumerated(EnumType.STRING)
    private ResourceBookingUnit resourceBookingUnit;

    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Min(0)
    @Column(name = "price")
    private Integer price;

    @Column(name = "label")
    private String label;

    @Column(name = "day_of_week")
    private Integer dayOfWeek;

    @Column(name = "starts_at")
    private LocalTime startsAt;

    @Column(name = "ends_at")
    private LocalTime endsAt;

    @Column(name = "adjustment_type")
    @Enumerated(EnumType.STRING)
    private ResourcePriceAdjustmentType adjustmentType;

    @jakarta.validation.constraints.Min(0)
    @Column(name = "adjustment_value")
    private Integer adjustmentValue;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "last_minute_minutes")
    private Integer lastMinuteMinutes;

    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Integer priority = 0;

    @Column(name = "active")
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = Boolean.FALSE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

}
