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

import java.time.Instant;
import java.time.LocalDate;

/** Edition definitive d'un etat : donnees figees, empreinte, PDF (lot F12). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_generated_report")
public class AccGeneratedReport {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "number")
    private String number;

    @Column(name = "report_code")
    private String reportCode;

    @Column(name = "fiscal_year_code")
    private String fiscalYearCode;

    @Column(name = "period_from")
    private LocalDate periodFrom;

    @Column(name = "period_to")
    private LocalDate periodTo;

    @Column(name = "template_version")
    private Integer templateVersion;

    @Column(name = "data_json")
    private String dataJson;

    @Column(name = "content_hash")
    private String contentHash;

    @Column(name = "pdf")
    private byte[] pdf;

    @Column(name = "derogation")
    private String derogation;

    @Column(name = "generated_by")
    private String generatedBy;

    @Builder.Default
    @Column(name = "generated_at")
    private Instant generatedAt = Instant.now();
}
