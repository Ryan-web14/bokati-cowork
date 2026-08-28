package com.sni.bokaticowork.features.payment.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.payment.enums.WalletEntryDirection;
import com.sni.bokaticowork.features.payment.enums.WalletEntryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
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
@Table(name = "wallet_ledger_entry")
public class WalletLedgerEntry {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "entry_number", nullable = false, unique = true, length = 100)
    private String entryNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false, foreignKey = @ForeignKey(name = "fk_wallet_ledger_wallet"))
    private WalletAccount wallet;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 20)
    private WalletEntryDirection direction;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 40)
    private WalletEntryType entryType;

    @Column(name = "source_type", length = 80)
    private String sourceType;

    @Column(name = "source_code", length = 120)
    private String sourceCode;

    @Column(name = "reference", length = 180)
    private String reference;

    /**
     * Cle de deduplication couverte par l'index unique partiel {@code ux_wallet_ledger_idempotency_key}.
     * Nulle lorsque l'operation n'a pas de source identifiable (recharge admin sans reference) :
     * on ne peut alors pas distinguer deux operations manuelles legitimes du meme montant.
     */
    @Column(name = "idempotency_key", length = 180)
    private String idempotencyKey;

    @Column(name = "created_by", length = 120)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }
}
