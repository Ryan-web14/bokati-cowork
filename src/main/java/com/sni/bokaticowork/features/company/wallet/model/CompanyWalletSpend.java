package com.sni.bokaticowork.features.company.wallet.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Une depense sur le portefeuille de l'organisation · le collaborateur qui l'a faite, la facture
 * qu'elle paie, et ou elle en est.
 *
 * <p>En attente de validation, ses fonds sont retenus sur le portefeuille ({@link #holdNumber}).
 * Executee, elle renvoie a la transaction de paiement.</p>
 */
@Entity
@Table(name = "wallet_company_spend")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyWalletSpend {

    public enum Status { PENDING_APPROVAL, EXECUTED, REJECTED, EXPIRED, CANCELLED }

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    /** Expose comme {@code id} par l'API · l'identifiant genere depasse ce que lit JavaScript. */
    @org.hibernate.annotations.Generated
    @Column(name = "short_id", insertable = false, updatable = false)
    private Long shortId;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    /** Vide quand le gerant paie lui-meme · il n'a pas besoin d'autorisation. */
    @Column(name = "authorization_id")
    private Long authorizationId;

    @Column(name = "customer_code", nullable = false, length = 120)
    private String companyCode;

    @Column(name = "spent_by_member", nullable = false, length = 120)
    private String spentByMember;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation", nullable = false, length = 30)
    private SpendOperation operation;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "document_number", nullable = false, length = 120)
    private String documentNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private Status status;

    @Column(name = "hold_number", length = 100)
    private String holdNumber;

    @Column(name = "intent_number", length = 100)
    private String intentNumber;

    @Column(name = "transaction_number", length = 100)
    private String transactionNumber;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "decided_by", length = 150)
    private String decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decision_reason", length = 500)
    private String decisionReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
