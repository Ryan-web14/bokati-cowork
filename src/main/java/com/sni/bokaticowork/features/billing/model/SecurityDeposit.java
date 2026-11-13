package com.sni.bokaticowork.features.billing.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Lot C7 : depot de garantie (caution) facture a un client, porte au compte 165. Il se restitue,
 * se retient (degats, impayes) ou s'impute sur une facture, en un ou plusieurs mouvements.
 * Statuts : HELD, PARTIALLY_SETTLED, SETTLED, CANCELLED.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "billing_security_deposit")
public class SecurityDeposit {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "deposit_number", nullable = false, unique = true, length = 40)
    private String depositNumber;

    /** Facture qui porte la caution. */
    @Column(name = "document_number", nullable = false, unique = true, length = 80)
    private String documentNumber;

    @Column(name = "subscription_number", length = 80)
    private String subscriptionNumber;

    @Column(name = "customer_type", length = 60)
    private String customerType;

    @Column(name = "customer_code", length = 120)
    private String customerCode;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "released_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal releasedAmount = BigDecimal.ZERO;

    @Column(name = "retained_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal retainedAmount = BigDecimal.ZERO;

    @Column(name = "applied_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal appliedAmount = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /**
     * Decision CD6 : caution d'abonnement creditee sur le portefeuille du client a l'encaissement de
     * sa facture (la retenue de portefeuille la bloque) ; ecriture du mouvement de portefeuille.
     */
    @Column(name = "wallet_entry_number", length = 60)
    private String walletEntryNumber;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Ce qui reste a restituer, retenir ou imputer. */
    public BigDecimal remaining() {
        return amount.subtract(releasedAmount).subtract(retainedAmount).subtract(appliedAmount);
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
