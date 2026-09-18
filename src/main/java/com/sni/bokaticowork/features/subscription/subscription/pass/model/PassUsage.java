package com.sni.bokaticowork.features.subscription.subscription.pass.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.pass.enums.PassUsageStatus;
import com.sni.bokaticowork.features.subscription.subscription.pass.enums.PassUsageType;
import com.sni.bokaticowork.features.subscription.subscription.pass.enums.PassValidationChannel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Ce qui s'est reellement passe quand quelqu'un a presente un pass.
 *
 * <p>C'etait le geste manquant. Le modele savait dire ce qu'un pass donnait ; rien ne consignait ce
 * qu'on en avait fait, ni quand, ni par qui, ni a quel guichet. Un pass a vingt journees dont on ne
 * sait pas combien ont ete utilisees n'est pas un pass, c'est une promesse.</p>
 *
 * <p>{@code usedByCode} n'est pas forcement le titulaire : un pass partage sert a plusieurs, et
 * savoir qui l'a employe est la seule facon de tenir les quotas individuels.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "pass_usage", indexes = {
        @Index(name = "idx_pass_usage_pass", columnList = "pass_id,status"),
        @Index(name = "idx_pass_usage_user", columnList = "used_by_type,used_by_code,created_at"),
        @Index(name = "idx_pass_usage_day", columnList = "pass_id,started_at")
})
public class PassUsage {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "usage_number", nullable = false, unique = true, length = 100)
    private String usageNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pass_id", nullable = false)
    private Pass pass;

    @Column(name = "used_by_type", length = 60)
    private String usedByType;

    @Column(name = "used_by_code", length = 120)
    private String usedByCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_type", nullable = false, length = 40)
    private PassUsageType usageType;

    @Column(name = "location_code", length = 120)
    private String locationCode;

    @Column(name = "resource_code", length = 120)
    private String resourceCode;

    @Column(name = "entitlement_code", length = 120)
    private String entitlementCode;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private PassUsageStatus status = PassUsageStatus.COMPLETED;

    @Column(name = "validated_by", length = 120)
    private String validatedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_channel", length = 40)
    private PassValidationChannel validationChannel;

    /**
     * Ce qui a declenche l'usage · une reservation, un pointage. Avec le code, il forme la clef qui
     * empeche qu'un double scan a la borne consomme deux journees.
     */
    @Column(name = "reference_type", length = 60)
    private String referenceType;

    @Column(name = "reference_code", length = 120)
    private String referenceCode;

    @Column(name = "reversed_at")
    private Instant reversedAt;

    @Column(name = "reversal_reason", length = 255)
    private String reversalReason;

    @Column(name = "reversed_by", length = 120)
    private String reversedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public boolean countsAgainstQuota() {
        return reversedAt == null
                && status != PassUsageStatus.CANCELLED;
    }

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        if (startedAt == null) {
            startedAt = createdAt;
        }
    }
}
