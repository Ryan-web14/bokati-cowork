package com.sni.bokaticowork.features.subscription.promotion.pricing.referral.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Le code qu'un parrain partage.
 *
 * <p>Un parrain n'a qu'un lien par programme. Lui en donner plusieurs eclaterait ses statistiques
 * sans rien lui apporter, et rendrait le plafond par parrain contournable.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "referral_link", indexes = {
        @Index(name = "idx_referral_link_referrer", columnList = "referrer_type,referrer_code")
})
public class ReferralLink {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    private ReferralProgram program;

    @Column(name = "referrer_type", nullable = false, length = 60)
    private String referrerType;

    @Column(name = "referrer_code", nullable = false, length = 120)
    private String referrerCode;

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "click_count", nullable = false)
    @Builder.Default
    private Integer clickCount = 0;

    /** Inscriptions issues du lien · le seul chiffre qui dise si le partage a servi a quelque chose. */
    @Column(name = "signup_count", nullable = false)
    @Builder.Default
    private Integer signupCount = 0;

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
