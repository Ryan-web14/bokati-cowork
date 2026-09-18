package com.sni.bokaticowork.features.payment.security.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.security.enums.WalletChallengeType;
import com.sni.bokaticowork.features.payment.security.enums.WalletConfirmationStatus;
import com.sni.bokaticowork.features.payment.security.enums.WalletOperationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Demande de confirmation d'une operation.
 *
 * <p>Le champ qui compte est {@code payloadHash}. Sans lui, un attaquant ferait confirmer un
 * transfert de mille francs et executerait un transfert de cent mille : l'empreinte lie la
 * confirmation a l'operation exacte, montant et destinataire compris, et rien ne peut changer entre
 * la demande et la reponse.</p>
 *
 * <p>L'echeance est courte, quelques minutes. Une confirmation qui traine est une confirmation
 * qu'on peut arracher a quelqu'un plus tard, hors du contexte ou il l'a demandee.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "wallet_transaction_confirmation", indexes = {
        @Index(name = "idx_wallet_confirmation_wallet", columnList = "wallet_id,status"),
        @Index(name = "idx_wallet_confirmation_expiry", columnList = "status,expires_at")
})
public class WalletTransactionConfirmation {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "confirmation_code", nullable = false, unique = true, length = 100)
    private String confirmationCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private WalletAccount wallet;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 60)
    private WalletOperationType operationType;

    @Column(name = "amount", precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency;

    /** Le nom du destinataire, montre au titulaire · le garde-fou le plus efficace contre l'erreur. */
    @Column(name = "counterparty_label", length = 255)
    private String counterpartyLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "challenge_type", nullable = false, length = 40)
    @Builder.Default
    private WalletChallengeType challengeType = WalletChallengeType.PIN;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private WalletConfirmationStatus status = WalletConfirmationStatus.PENDING;

    @Column(name = "payload_hash", nullable = false, length = 128)
    private String payloadHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "attempts", nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    @Column(name = "max_attempts", nullable = false)
    @Builder.Default
    private Integer maxAttempts = 3;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "ip_address", length = 60)
    private String ipAddress;

    @Column(name = "device_id", length = 120)
    private String deviceId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public boolean usableAt(Instant moment) {
        return status == WalletConfirmationStatus.PENDING
                && moment.isBefore(expiresAt)
                && attempts < maxAttempts;
    }

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }
}
