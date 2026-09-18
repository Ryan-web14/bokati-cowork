package com.sni.bokaticowork.features.subscription.subscription.pass.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.pass.enums.PassCredentialType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Le support par lequel un pass se presente.
 *
 * <p>Distinct du pass lui-meme, et c'est ce qui permet de revoquer un code sans annuler le droit :
 * un telephone perdu appelle un nouveau support, pas un nouveau pass.</p>
 *
 * <p>La valeur d'un support actif est unique · c'est par elle qu'on retrouve le pass a la borne, et
 * deux pass partageant un code rendraient la lecture ambigue.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "pass_credential", indexes = {
        @Index(name = "idx_pass_credential_pass", columnList = "pass_id,revoked_at")
})
public class PassCredential {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pass_id", nullable = false)
    private Pass pass;

    @Enumerated(EnumType.STRING)
    @Column(name = "credential_type", nullable = false, length = 40)
    private PassCredentialType credentialType;

    @Column(name = "value", nullable = false, length = 255)
    private String value;

    /** Un code tournant limite la copie d'ecran · le support change, le pass reste. */
    @Column(name = "rotating", nullable = false)
    @Builder.Default
    private Boolean rotating = Boolean.FALSE;

    @Column(name = "valid_until")
    private Instant validUntil;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "issued_by", length = 120)
    private String issuedBy;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_reason", length = 255)
    private String revokedReason;

    public boolean usableAt(Instant moment) {
        return revokedAt == null && (validUntil == null || !moment.isAfter(validUntil));
    }

    @PrePersist
    public void prePersist() {
        if (issuedAt == null) {
            issuedAt = Instant.now();
        }
    }
}
