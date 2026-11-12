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

/** Journal comptable et ses regles. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_journal")
public class AccJournal {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Builder.Default
    @Column(name = "company_code", nullable = false, length = 40)
    private String companyCode = "MAIN";

    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "label", nullable = false, length = 255)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private com.sni.bokaticowork.features.accounting.enums.JournalType type;

    @Column(name = "treasury_account_number", length = 20)
    private String treasuryAccountNumber;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "numbering_reset", nullable = false, length = 10)
    private com.sni.bokaticowork.features.accounting.enums.NumberingReset numberingReset = com.sni.bokaticowork.features.accounting.enums.NumberingReset.YEAR;

    @Builder.Default
    @Column(name = "auto_validate", nullable = false)
    private Boolean autoValidate = Boolean.TRUE;

    @Column(name = "allowed_accounts", length = 2000)
    private String allowedAccounts;

    @Column(name = "cash_register_code", length = 100)
    private String cashRegisterCode;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_by", length = 180)
    private String createdBy;

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
