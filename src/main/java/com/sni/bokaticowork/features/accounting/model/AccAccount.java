package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Compte du plan de comptes de l'entreprise. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_account")
public class AccAccount {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Builder.Default
    @Column(name = "company_code", nullable = false, length = 40)
    private String companyCode = "MAIN";

    @Column(name = "number", nullable = false, unique = true, length = 20)
    private String number;

    @Column(name = "label", nullable = false, length = 255)
    private String label;

    @Column(name = "parent_number", length = 20)
    private String parentNumber;

    @Column(name = "class_code", nullable = false, length = 1)
    private String classCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "nature", nullable = false, length = 20)
    private com.sni.bokaticowork.features.accounting.enums.AccountNature nature;

    @Enumerated(EnumType.STRING)
    @Column(name = "normal_side", nullable = false, length = 6)
    private com.sni.bokaticowork.features.accounting.enums.AccountSide normalSide;

    @Builder.Default
    @Column(name = "reconcilable", nullable = false)
    private Boolean reconcilable = Boolean.FALSE;

    @Enumerated(EnumType.STRING)
    @Column(name = "third_party_type", length = 20)
    private com.sni.bokaticowork.features.accounting.enums.ThirdPartyType thirdPartyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "treasury_type", length = 20)
    private com.sni.bokaticowork.features.accounting.enums.TreasuryType treasuryType;

    @Builder.Default
    @Column(name = "analytic_required", nullable = false)
    private Boolean analyticRequired = Boolean.FALSE;

    @Column(name = "tax_code", length = 30)
    private String taxCode;

    @Column(name = "statement_line_code", length = 40)
    private String statementLineCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private com.sni.bokaticowork.features.accounting.enums.AccountStatus status;

    @Column(name = "merged_into", length = 20)
    private String mergedInto;

    @Column(name = "created_by", length = 180)
    private String createdBy;

    @Column(name = "approved_by", length = 180)
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "updated_by", length = 180)
    private String updatedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
