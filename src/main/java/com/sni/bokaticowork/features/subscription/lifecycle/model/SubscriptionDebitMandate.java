package com.sni.bokaticowork.features.subscription.lifecycle.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Un mandat de prelevement · l'accord prealable sans lequel on ne touche pas au portefeuille.
 *
 * <p>Donne par l'abonne, par un canal nomme, avec une reference. Il peut plafonner chaque
 * prelevement. Il se revoque, jamais ne s'efface : la question « avions-nous le droit » a toujours
 * une reponse.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "subscription_debit_mandate")
public class SubscriptionDebitMandate {

    public enum Status {
        ACTIVE, SUSPENDED, REVOKED
    }

    public enum Channel {
        PORTAL, SIGNED_FORM, EMAIL, STAFF
    }

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "mandate_code", nullable = false, unique = true, length = 40)
    private String mandateCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "wallet_number", nullable = false, length = 60)
    private String walletNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @Column(name = "consent_given_at", nullable = false)
    private Instant consentGivenAt;

    @Column(name = "consent_given_by", nullable = false, length = 120)
    private String consentGivenBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_channel", nullable = false, length = 20)
    private Channel consentChannel;

    @Column(name = "consent_reference", length = 120)
    private String consentReference;

    @Column(name = "max_amount_per_debit", precision = 19, scale = 4)
    private BigDecimal maxAmountPerDebit;

    @Column(name = "consecutive_failures", nullable = false)
    @Builder.Default
    private Integer consecutiveFailures = 0;

    @Column(name = "last_debit_at")
    private Instant lastDebitAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_by", length = 120)
    private String revokedBy;

    @Column(name = "revocation_reason", columnDefinition = "TEXT")
    private String revocationReason;

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
