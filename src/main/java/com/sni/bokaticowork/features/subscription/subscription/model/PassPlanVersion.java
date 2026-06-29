package com.sni.bokaticowork.features.subscription.subscription.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassDurationUnit;
import com.sni.bokaticowork.features.subscription.subscription.enums.PlanStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnTransformer;

import java.time.Instant;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "pass_plan_version", indexes = {
        @Index(name = "idx_pass_plan_version_plan", columnList = "plan_id"),
        @Index(name = "idx_pass_plan_version_status", columnList = "status")
})
public class PassPlanVersion {

    @Id
    @IdGeneration
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_pass_plan_version_plan"))
    private PassPlan plan;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private PlanStatus status = PlanStatus.DRAFT;

    @Column(name = "duration", nullable = false)
    private Integer duration;

    @Enumerated(EnumType.STRING)
    @Column(name = "duration_unit", nullable = false, length = 20)
    private PassDurationUnit durationUnit;

    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "auto_renewable", nullable = false)
    @Builder.Default
    private Boolean autoRenewable = Boolean.FALSE;

    @Column(name = "required_kyc_level", nullable = false)
    @Builder.Default
    private Integer requiredKycLevel = 1;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "terms_json", columnDefinition = "jsonb")
    private String termsJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() { createdAt = updatedAt = Instant.now(); }

    @PreUpdate
    public void preUpdate() { updatedAt = Instant.now(); }
}
