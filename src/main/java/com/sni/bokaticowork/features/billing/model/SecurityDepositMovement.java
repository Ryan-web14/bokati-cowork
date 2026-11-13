package com.sni.bokaticowork.features.billing.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Lot C7 : mouvement d'une caution, RELEASE (restitution), RETAIN (retenue) ou APPLY (imputation). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "billing_security_deposit_movement")
public class SecurityDepositMovement {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "movement_number", nullable = false, unique = true, length = 40)
    private String movementNumber;

    @Column(name = "deposit_id", nullable = false)
    private Long depositId;

    @Column(name = "kind", nullable = false, length = 20)
    private String kind;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "target_document_number", length = 80)
    private String targetDocumentNumber;

    @Column(name = "movement_date", nullable = false)
    private LocalDate movementDate;

    /** Decision CD6 : regle par le portefeuille (sortie d'abonnement) ; pas d'ecriture propre. */
    @Column(name = "wallet_settled", nullable = false)
    @Builder.Default
    private Boolean walletSettled = Boolean.FALSE;

    @Column(name = "created_by", length = 180)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }
}
