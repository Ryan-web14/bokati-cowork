package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Lot d'export d'ecritures ou de balance, trace avec son empreinte (lot F14). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_export_batch")
public class AccExportBatch {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "number")
    private String number;

    @Column(name = "format")
    private String format;

    @Column(name = "mode")
    private String mode;

    @Column(name = "period_from")
    private LocalDate periodFrom;

    @Column(name = "period_to")
    private LocalDate periodTo;

    @Column(name = "journal_code")
    private String journalCode;

    @Builder.Default
    @Column(name = "entry_count")
    private Integer entryCount = 0;

    @Builder.Default
    @Column(name = "line_count")
    private Integer lineCount = 0;

    @Builder.Default
    @Column(name = "total_debit")
    private BigDecimal totalDebit = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_credit")
    private BigDecimal totalCredit = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "excluded")
    private Integer excluded = 0;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "content_hash")
    private String contentHash;

    @Column(name = "created_by")
    private String createdBy;

    @Builder.Default
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}
