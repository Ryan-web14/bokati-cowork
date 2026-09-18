package com.sni.bokaticowork.features.payment.security.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Le secret d'un portefeuille · son empreinte, jamais le code.
 *
 * <p>Un administrateur ne peut pas le lire, seulement le reinitialiser. L'algorithme est conserve a
 * cote de l'empreinte pour pouvoir en changer plus tard sans invalider les codes existants : sans
 * lui, une migration de hachage obligerait a redemander son code a tout le monde le meme jour.</p>
 *
 * <p>Le verrouillage s'aggrave a chaque episode. Une temporisation fixe se contourne par la
 * patience ; une temporisation qui double rend l'essai automatise sans interet au bout de quelques
 * tours, sans jamais bloquer definitivement un titulaire qui a simplement oublie.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "wallet_credential")
public class WalletCredential {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false, unique = true)
    private WalletAccount wallet;

    @Column(name = "pin_hash", nullable = false, length = 255)
    private String pinHash;

    @Column(name = "pin_algorithm", nullable = false, length = 60)
    private String pinAlgorithm;

    @Column(name = "pin_set_at", nullable = false)
    private Instant pinSetAt;

    @Column(name = "pin_expires_at")
    private Instant pinExpiresAt;

    @Column(name = "failed_attempts", nullable = false)
    @Builder.Default
    private Integer failedAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    /** Nombre de verrouillages subis · c'est lui qui fait croitre la temporisation. */
    @Column(name = "lockout_count", nullable = false)
    @Builder.Default
    private Integer lockoutCount = 0;

    /** Vrai apres une reinitialisation administrative · le titulaire doit en choisir un nouveau. */
    @Column(name = "must_change_pin", nullable = false)
    @Builder.Default
    private Boolean mustChangePin = Boolean.FALSE;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public boolean lockedAt(Instant moment) {
        return lockedUntil != null && moment.isBefore(lockedUntil);
    }

    public boolean expiredAt(Instant moment) {
        return pinExpiresAt != null && moment.isAfter(pinExpiresAt);
    }

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (pinSetAt == null) {
            pinSetAt = now;
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
