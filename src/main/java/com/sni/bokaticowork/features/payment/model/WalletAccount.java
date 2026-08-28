package com.sni.bokaticowork.features.payment.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.payment.enums.WalletStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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
@Table(name = "wallet_account", indexes = {
        @Index(name = "idx_wallet_account_number", columnList = "wallet_number"),
        @Index(name = "idx_wallet_account_owner", columnList = "owner_type,owner_code,currency")
})
public class WalletAccount {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "wallet_number", nullable = false, unique = true, length = 100)
    private String walletNumber;

    @Column(name = "owner_type", nullable = false, length = 60)
    private String ownerType;

    @Column(name = "owner_code", nullable = false, length = 120)
    private String ownerCode;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private WalletStatus status;

    @Column(name = "available_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal availableBalance;

    @Column(name = "ledger_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal ledgerBalance;

    @Column(name = "held_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal heldBalance;

    /**
     * Verrou optimiste · seconde ligne de defense derriere le SELECT ... FOR UPDATE pose par
     * {@code WalletLedgerService}. Tout chemin d'ecriture qui oublierait le verrou pessimiste
     * echoue au commit au lieu d'ecraser le solde ecrit par une transaction concurrente.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        openedAt = openedAt == null ? now : openedAt;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
