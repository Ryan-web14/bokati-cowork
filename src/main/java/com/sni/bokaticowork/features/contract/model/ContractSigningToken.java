package com.sni.bokaticowork.features.contract.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "contract_signing_token")
public class ContractSigningToken {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Builder.Default
    @Column(name = "token", nullable = false, unique = true)
    private UUID token = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Column(name = "contract_code", nullable = false, length = 120)
    private String contractCode;

    @Column(name = "signer_email", nullable = false, length = 300)
    private String signerEmail;

    @Column(name = "signer_name", length = 300)
    private String signerName;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "signed_at")
    private Instant signedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    @Column(name = "consent_text", columnDefinition = "text")
    private String consentText;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Builder.Default
    @Column(name = "revoked", nullable = false)
    private Boolean revoked = false;

    @PrePersist
    public void prePersist() {
        if (token == null) token = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        if (revoked == null) revoked = false;
    }
}
