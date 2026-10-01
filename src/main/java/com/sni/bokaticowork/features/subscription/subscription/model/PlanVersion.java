package com.sni.bokaticowork.features.subscription.subscription.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.subscription.enums.PlanStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "subscription_plan_version", indexes = {
        @Index(name = "idx_plan_version_plan", columnList = "plan_id"),
        @Index(name = "idx_plan_version_status", columnList = "status")
})
public class PlanVersion {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false, foreignKey = @ForeignKey(name = "fk_plan_version_plan"))
    private SubscriptionPlan plan;

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

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @org.hibernate.annotations.ColumnTransformer(write = "?::jsonb")
    @Column(name = "terms_json", columnDefinition = "jsonb")
    private String termsJson;

    /** CATALOGUE pour tout le monde · SUBSCRIPTION pour un seul abonnement, hors catalogue. */
    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    @Builder.Default
    private Scope scope = Scope.CATALOGUE;

    /** Renseigne seulement pour une version privee · l'abonnement a qui elle appartient. */
    @Column(name = "owner_subscription_id")
    private Long ownerSubscriptionId;

    /** La version de catalogue dont celle-ci descend · le lien qui evite les orphelins. */
    @Column(name = "derived_from_version_id")
    private Long derivedFromVersionId;

    /** Prix plancher d'une version de catalogue · aucune derivation en dessous, meme approuvee. */
    @Column(name = "floor_price", precision = 19, scale = 4)
    private BigDecimal floorPrice;

    public enum Scope {
        CATALOGUE, SUBSCRIPTION
    }

    public boolean privateToSubscription() {
        return scope == Scope.SUBSCRIPTION;
    }

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
