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

    /**
     * Identifiant technique de la transaction · version 7, donc horodate et croissant.
     *
     * <p>Distinct de la cle d'idempotence, qui appartient a l'appelant et peut etre nulle. Celui-ci
     * appartient a l'ecriture et ne l'est jamais.</p>
     */
    @Column(name = "transaction_uuid", unique = true)
    private java.util.UUID transactionUuid;

    /** Reference lisible, prononcable au telephone · ne sert jamais de clef etrangere. */
    @Column(name = "transaction_number", unique = true, length = 100)
    private String transactionNumber;

    @Column(name = "previous_hash", length = 128)
    private String previousHash;

    /**
     * Empreinte de cette ecriture et de celle qui la precede.
     *
     * <p>Modifier une ecriture passee sans rompre la chaine supposerait de recalculer toutes les
     * suivantes · le declencheur d'immuabilite l'interdit deja, le chainage le rend detectable
     * meme si quelqu'un desactivait ce declencheur.</p>
     */
    @Column(name = "current_hash", length = 128)
    private String currentHash;

    @Column(name = "created_by", length = 120)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        // Pose seulement si l'appelant ne l'a pas fixe : l'instant entre dans l'empreinte, qui est
        // calculee avant la persistance · l'ecraser ici invaliderait le chainage des sa creation.
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
