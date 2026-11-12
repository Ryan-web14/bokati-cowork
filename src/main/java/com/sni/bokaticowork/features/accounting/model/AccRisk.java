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

/** Fiche du registre des risques et sa provision (lot F19). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_risk")
public class AccRisk {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code")
    private String code;

    @Column(name = "nature")
    private String nature;

    @Column(name = "title")
    private String title;

    @Column(name = "third_party")
    private String thirdParty;

    @Column(name = "contract_ref")
    private String contractRef;

    @Column(name = "origin_ref")
    private String originRef;

    @Column(name = "description")
    private String description;

    @Column(name = "counsel")
    private String counsel;

    @Column(name = "jurisdiction")
    private String jurisdiction;

    @Column(name = "next_deadline")
    private LocalDate nextDeadline;

    @Builder.Default
    @Column(name = "probability")
    private String probability = "POSSIBLE";

    @Builder.Default
    @Column(name = "estimated_amount")
    private BigDecimal estimatedAmount = BigDecimal.ZERO;

    @Column(name = "estimation_method")
    private String estimationMethod;

    @Column(name = "estimated_at")
    private LocalDate estimatedAt;

    @Column(name = "outflow_date")
    private LocalDate outflowDate;

    @Builder.Default
    @Column(name = "term")
    private String term = "LONG";

    @Column(name = "provision_account")
    private String provisionAccount;

    @Builder.Default
    @Column(name = "provision_amount")
    private BigDecimal provisionAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "deductible")
    private Boolean deductible = Boolean.TRUE;

    @Column(name = "section_code")
    private String sectionCode;

    @Column(name = "documents")
    private String documents;

    @Builder.Default
    @Column(name = "status")
    private String status = "OPEN";

    @Column(name = "created_by")
    private String createdBy;

    @Builder.Default
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Builder.Default
    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();
}
