package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Modele d'import d'un releve CSV (lot F10). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "bank_import_template")
public class BankImportTemplate {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "label", nullable = false)
    private String label;

    @Builder.Default
    @Column(name = "format", nullable = false)
    private String format = "CSV";

    @Builder.Default
    @Column(name = "delimiter", nullable = false)
    private String delimiter = ";";

    @Builder.Default
    @Column(name = "header_rows", nullable = false)
    private Integer headerRows = 1;

    @Column(name = "date_column", nullable = false)
    private String dateColumn;

    @Column(name = "value_date_column")
    private String valueDateColumn;

    @Column(name = "label_column", nullable = false)
    private String labelColumn;

    @Column(name = "reference_column")
    private String referenceColumn;

    @Column(name = "amount_column")
    private String amountColumn;

    @Column(name = "debit_column")
    private String debitColumn;

    @Column(name = "credit_column")
    private String creditColumn;

    @Builder.Default
    @Column(name = "date_format", nullable = false)
    private String dateFormat = "dd/MM/yyyy";

    @Builder.Default
    @Column(name = "decimal_separator", nullable = false)
    private String decimalSeparator = ",";

    @Builder.Default
    @Column(name = "encoding", nullable = false)
    private String encoding = "UTF-8";

    @Column(name = "created_at", nullable = false)
    private java.time.Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = java.time.Instant.now();
        }
    }
}
