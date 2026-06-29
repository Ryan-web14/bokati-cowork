package com.sni.bokaticowork.features.subscription.subscription.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassType;
import com.sni.bokaticowork.features.subscription.subscription.enums.PlanStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.TargetAudience;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "pass_plan", indexes = {
        @Index(name = "idx_pass_plan_code", columnList = "code"),
        @Index(name = "idx_pass_plan_status", columnList = "status")
})
@SQLDelete(sql = "UPDATE pass_plan SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
public class PassPlan {

    @Id
    @IdGeneration
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 80)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "pass_type", nullable = false, length = 60)
    private PassType passType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_audience", nullable = false, length = 60)
    private TargetAudience targetAudience;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private PlanStatus status = PlanStatus.DRAFT;

    @Column(name = "visible", nullable = false)
    @Builder.Default
    private Boolean visible = Boolean.FALSE;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "required_kyc_level", nullable = false)
    @Builder.Default
    private Integer requiredKycLevel = 1;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = Boolean.FALSE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() { createdAt = updatedAt = Instant.now(); }

    @PreUpdate
    public void preUpdate() { updatedAt = Instant.now(); }
}
