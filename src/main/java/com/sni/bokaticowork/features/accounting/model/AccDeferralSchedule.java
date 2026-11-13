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

/** Echeancier de reconnaissance d'une ligne de facture ou de charge (lot F20). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_deferral_schedule")
public class AccDeferralSchedule {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Builder.Default
    @Column(name = "company_code")
    private String companyCode = "MAIN";

    @Column(name = "kind")
    private String kind;

    @Column(name = "source_type")
    private String sourceType;

    @Column(name = "source_line_id")
    private Long sourceLineId;

    @Column(name = "document_number")
    private String documentNumber;

    @Column(name = "third_party")
    private String thirdParty;

    @Column(name = "label")
    private String label;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "section_code")
    private String sectionCode;

    @Column(name = "activity")
    private String activity;

    @Builder.Default
    @Column(name = "amount")
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "method")
    private String method;

    @Column(name = "total_units")
    private BigDecimal totalUnits;

    @Column(name = "recognition_source")
    private String recognitionSource;

    @Builder.Default
    @Column(name = "version")
    private Integer version = 1;

    @Builder.Default
    @Column(name = "status")
    private String status = "ACTIVE";

    @Column(name = "ended_on")
    private LocalDate endedOn;

    @Builder.Default
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Builder.Default
    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    /** Reference de la source : numero de pass pour la reconnaissance a l'usage. */
    @Column(name = "source_ref")
    private String sourceRef;
}
