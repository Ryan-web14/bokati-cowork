package com.sni.bokaticowork.features.payment.transfer.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Un destinataire enregistre.
 *
 * <p>Un alias, un portefeuille cible, et le compte des envois deja faits. Ce compte n'est pas
 * decoratif : un premier envoi vers un inconnu et un dixieme vers un habitue ne meritent pas la
 * meme vigilance, et la surveillance lira cette colonne.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "wallet_beneficiary")
public class WalletBeneficiary {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_wallet_id", nullable = false)
    private WalletAccount ownerWallet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "beneficiary_wallet_id", nullable = false)
    private WalletAccount beneficiaryWallet;

    @Column(name = "alias", nullable = false, length = 120)
    private String alias;

    @Column(name = "transfer_count", nullable = false)
    @Builder.Default
    private Integer transferCount = 0;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }
}
