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
 * Un appareil qui a deja opere sur ce portefeuille.
 *
 * <p>Un appareil jamais vu qui tente un transfert important est le signal le plus simple et le
 * plus fiable dont on dispose · il ne demande aucune intelligence, seulement d'avoir tenu la liste.
 * Le titulaire peut revoquer un appareil : un telephone perdu cesse alors d'etre « connu » meme si
 * quelqu'un s'en sert encore.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "wallet_device")
public class WalletDevice {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private WalletAccount wallet;

    @Column(name = "device_id", nullable = false, length = 120)
    private String deviceId;

    @Column(name = "label", length = 120)
    private String label;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "last_ip_address", length = 60)
    private String lastIpAddress;

    @Column(name = "use_count", nullable = false)
    @Builder.Default
    private Integer useCount = 1;

    @Column(name = "trusted", nullable = false)
    @Builder.Default
    private Boolean trusted = Boolean.FALSE;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public boolean revoked() {
        return revokedAt != null;
    }

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        firstSeenAt = firstSeenAt == null ? now : firstSeenAt;
        lastSeenAt = lastSeenAt == null ? now : lastSeenAt;
    }
}
