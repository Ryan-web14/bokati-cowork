package com.sni.bokaticowork.features.subscription.subscription.pass.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.pass.enums.PassBeneficiaryRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Qui a le droit de se servir d'un pass.
 *
 * <p>Sans cette table, {@code COMPANY_SHARED_PASS} reste un mot. Une entreprise achete vingt
 * journees, designe cinq collaborateurs, et peut plafonner chacun a six.</p>
 *
 * <p>Le quota individuel vit <b>dans</b> le quota global : la somme des quotas peut le depasser, et
 * c'est alors le premier arrive qui consomme. C'est voulu · plafonner chacun a la part exacte
 * gaspillerait le solde de celui qui ne vient pas.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "pass_beneficiary", indexes = {
        @Index(name = "idx_pass_beneficiary_person", columnList = "beneficiary_type,beneficiary_code,revoked_at")
})
public class PassBeneficiary {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pass_id", nullable = false)
    private Pass pass;

    @Column(name = "beneficiary_type", nullable = false, length = 60)
    private String beneficiaryType;

    @Column(name = "beneficiary_code", nullable = false, length = 120)
    private String beneficiaryCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 40)
    @Builder.Default
    private PassBeneficiaryRole role = PassBeneficiaryRole.AUTHORISED_USER;

    @Column(name = "max_uses_for_beneficiary")
    private Integer maxUsesForBeneficiary;

    @Column(name = "used_count", nullable = false)
    @Builder.Default
    private Integer usedCount = 0;

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_until")
    private Instant validUntil;

    @Column(name = "added_by", length = 120)
    private String addedBy;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_by", length = 120)
    private String revokedBy;

    @Column(name = "revoked_reason", length = 255)
    private String revokedReason;

    public boolean usableAt(Instant moment) {
        return revokedAt == null
                && (validFrom == null || !moment.isBefore(validFrom))
                && (validUntil == null || !moment.isAfter(validUntil));
    }

    /** Quota individuel restant, ou {@code null} si le beneficiaire n'est pas plafonne a titre propre. */
    public Integer remainingForBeneficiary() {
        if (maxUsesForBeneficiary == null) {
            return null;
        }
        return Math.max(0, maxUsesForBeneficiary - (usedCount == null ? 0 : usedCount));
    }

    @PrePersist
    public void prePersist() {
        if (addedAt == null) {
            addedAt = Instant.now();
        }
    }
}
