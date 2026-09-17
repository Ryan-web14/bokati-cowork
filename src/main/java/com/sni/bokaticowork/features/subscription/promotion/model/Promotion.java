package com.sni.bokaticowork.features.subscription.promotion.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.promotion.enums.DiscountType;
import com.sni.bokaticowork.features.subscription.promotion.enums.PromotionStatus;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.PromotionType;
import jakarta.persistence.*;
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
@Table(name = "promotion", indexes = {
        @Index(name = "idx_promotion_code", columnList = "code"),
        @Index(name = "idx_promotion_status_dates", columnList = "status,starts_at,ends_at")
})
public class Promotion {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 60)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal discountValue;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(name = "max_redemptions")
    private Integer maxRedemptions;

    @Column(name = "redemption_count", nullable = false)
    @Builder.Default
    private Integer redemptionCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private PromotionStatus status = PromotionStatus.DRAFT;

    // ---------------------------------------------------------------------------------------
    // Comment la promotion se declenche, se combine, et ce qu'elle a le droit de couter
    // ---------------------------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "promotion_type", nullable = false, length = 40)
    @Builder.Default
    private PromotionType promotionType = PromotionType.AUTOMATIC;

    /** La promotion accepte-t-elle de s'ajouter a une autre deja retenue. */
    @Column(name = "stackable", nullable = false)
    @Builder.Default
    private Boolean stackable = Boolean.TRUE;

    /** Si elle s'applique, l'evaluation des suivantes s'arrete. */
    @Column(name = "exclusive", nullable = false)
    @Builder.Default
    private Boolean exclusive = Boolean.FALSE;

    /** Ordre croissant d'evaluation. A egalite, la plus avantageuse pour le client l'emporte. */
    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Integer priority = 100;

    @Column(name = "max_redemptions_per_subscriber")
    private Integer maxRedemptionsPerSubscriber;

    /**
     * Plafond absolu, meme pour un pourcentage. Quatre-vingts pour cent sur un abonnement annuel
     * d'entreprise n'est presque jamais l'intention de celui qui a saisi la campagne.
     */
    @Column(name = "max_discount_amount", precision = 19, scale = 4)
    private BigDecimal maxDiscountAmount;

    /** Enveloppe de la campagne. Nulle si elle n'en a pas. */
    @Column(name = "budget_amount", precision = 19, scale = 4)
    private BigDecimal budgetAmount;

    @Column(name = "consumed_budget_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal consumedBudgetAmount = BigDecimal.ZERO;

    @Column(name = "total_discount_granted", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalDiscountGranted = BigDecimal.ZERO;

    /** Compte de contrepartie, pour que le cout de la campagne se retrouve en comptabilite. */
    @Column(name = "counterpart_account", length = 60)
    private String counterpartAccount;

    @Column(name = "approved_by", length = 120)
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    /**
     * Verrou optimiste sur le budget. Deux paniers simultanes ne peuvent pas consommer deux fois la
     * meme enveloppe restante, ce qui est exactement ce qui arrive a une campagne qui marche.
     */
    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    @org.hibernate.annotations.ColumnTransformer(write = "?::jsonb")
    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private String metadataJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Enveloppe encore disponible, ou {@code null} lorsque la campagne n'a pas de budget. Zero et
     * absence de budget ne veulent pas dire la meme chose : le premier arrete la campagne, le
     * second ne l'a jamais bornee.
     */
    public BigDecimal remainingBudget() {
        if (budgetAmount == null) {
            return null;
        }
        BigDecimal consumed = consumedBudgetAmount == null ? BigDecimal.ZERO : consumedBudgetAmount;
        BigDecimal remaining = budgetAmount.subtract(consumed);
        return remaining.signum() < 0 ? BigDecimal.ZERO : remaining;
    }

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
