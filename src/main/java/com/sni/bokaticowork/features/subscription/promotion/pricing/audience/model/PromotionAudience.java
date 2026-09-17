package com.sni.bokaticowork.features.subscription.promotion.pricing.audience.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.promotion.model.Promotion;
import com.sni.bokaticowork.features.subscription.promotion.pricing.audience.enums.PromotionAudienceType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** A qui une promotion s'adresse. Une promotion sans audience s'adresse a tous. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "promotion_audience", indexes = {
        @Index(name = "idx_promotion_audience_promotion", columnList = "promotion_id")
})
public class PromotionAudience {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @Enumerated(EnumType.STRING)
    @Column(name = "audience_type", nullable = false, length = 40)
    private PromotionAudienceType audienceType;

    @Column(name = "segment_code", length = 120)
    private String segmentCode;

    @Column(name = "plan_code", length = 120)
    private String planCode;

    @Column(name = "business_entity_code", length = 120)
    private String businessEntityCode;

    /** Regle de cohorte, en donnees. Evaluee a l'admission, pas a la volee dans le moteur. */
    @Column(name = "cohort_rule", columnDefinition = "text")
    private String cohortRule;

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
