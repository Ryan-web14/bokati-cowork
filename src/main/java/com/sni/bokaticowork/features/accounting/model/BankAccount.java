package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Compte bancaire ou mobile money suivi en rapprochement (lot F10). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "bank_account")
public class BankAccount {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false)
    private String code;

    @Builder.Default
    @Column(name = "kind", nullable = false)
    private String kind = "BANK";

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "branch")
    private String branch;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "rib")
    private String rib;

    @Builder.Default
    @Column(name = "currency", nullable = false)
    private String currency = "XAF";

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Column(name = "journal_code", nullable = false)
    private String journalCode;

    @Column(name = "import_template_code")
    private String importTemplateCode;

    @Builder.Default
    @Column(name = "opening_balance", nullable = false)
    private java.math.BigDecimal openingBalance = java.math.BigDecimal.ZERO;

    @Column(name = "opening_date")
    private java.time.LocalDate openingDate;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private java.time.Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = java.time.Instant.now();
        }
    }
}
