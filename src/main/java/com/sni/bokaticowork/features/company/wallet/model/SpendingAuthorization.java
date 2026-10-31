package com.sni.bokaticowork.features.company.wallet.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ce qu'un collaborateur peut depenser sur le portefeuille de son organisation.
 *
 * <p>Chaque plafond est facultatif. Au-dela de {@link #approvalAbove}, la depense attend la
 * validation du gerant.</p>
 */
@Entity
@Table(name = "wallet_spending_authorization")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpendingAuthorization {

    public enum Status { ACTIVE, SUSPENDED, REVOKED }

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    /** Expose comme {@code id} par l'API · l'identifiant genere depasse ce que lit JavaScript. */
    @org.hibernate.annotations.Generated
    @Column(name = "short_id", insertable = false, updatable = false)
    private Long shortId;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Column(name = "customer_code", nullable = false, length = 120)
    private String companyCode;

    @Column(name = "member_code", nullable = false, length = 120)
    private String memberCode;

    @Column(name = "allowed_operations", nullable = false, length = 200)
    private String allowedOperations;

    @Column(name = "max_per_transaction", precision = 19, scale = 4)
    private BigDecimal maxPerTransaction;

    @Column(name = "max_per_day", precision = 19, scale = 4)
    private BigDecimal maxPerDay;

    @Column(name = "max_per_month", precision = 19, scale = 4)
    private BigDecimal maxPerMonth;

    @Column(name = "approval_above", precision = 19, scale = 4)
    private BigDecimal approvalAbove;

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_until")
    private Instant validUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "status_reason", length = 500)
    private String statusReason;

    @Column(name = "created_by", length = 150)
    private String createdBy;

    @Column(name = "updated_by", length = 150)
    private String updatedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Set<SpendOperation> operations() {
        if (allowedOperations == null || allowedOperations.isBlank()) {
            return EnumSet.noneOf(SpendOperation.class);
        }
        return Arrays.stream(allowedOperations.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .map(SpendOperation::valueOf).collect(Collectors.toCollection(() -> EnumSet.noneOf(SpendOperation.class)));
    }

    public void setOperations(Set<SpendOperation> operations) {
        this.allowedOperations = operations.stream().sorted().map(Enum::name).collect(Collectors.joining(","));
    }

    /** En vigueur a cet instant · active et dans sa periode. */
    public boolean inForceAt(Instant moment) {
        return status == Status.ACTIVE
                && (validFrom == null || !moment.isBefore(validFrom))
                && (validUntil == null || !moment.isAfter(validUntil));
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
