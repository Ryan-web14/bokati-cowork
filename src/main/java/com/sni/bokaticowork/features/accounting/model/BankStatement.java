package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Releve importe, fige. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "bank_statement")
public class BankStatement {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "number", nullable = false)
    private String number;

    @Column(name = "bank_account_code", nullable = false)
    private String bankAccountCode;

    @Column(name = "period_from", nullable = false)
    private java.time.LocalDate periodFrom;

    @Column(name = "period_to", nullable = false)
    private java.time.LocalDate periodTo;

    @Column(name = "opening_balance", nullable = false)
    private java.math.BigDecimal openingBalance;

    @Column(name = "closing_balance", nullable = false)
    private java.math.BigDecimal closingBalance;

    @Column(name = "file_name")
    private String fileName;

    @Builder.Default
    @Column(name = "source", nullable = false)
    private String source = "FILE";

    @Builder.Default
    @Column(name = "missing_before", nullable = false)
    private Boolean missingBefore = Boolean.FALSE;

    @Builder.Default
    @Column(name = "duplicates", nullable = false)
    private Integer duplicates = 0;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private String status = "IMPORTED";

    @Column(name = "replaced_by")
    private String replacedBy;

    @Column(name = "imported_by", nullable = false)
    private String importedBy;

    @Column(name = "imported_at", nullable = false)
    private java.time.Instant importedAt;

    @PrePersist
    void prePersist() {
        if (importedAt == null) {
            importedAt = java.time.Instant.now();
        }
    }
}
