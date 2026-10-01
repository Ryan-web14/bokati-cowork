package com.sni.bokaticowork.features.domiciliation.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * L'enregistrement du contrat aupres de l'administration · une demarche externe, suivie a part.
 *
 * <p>Separer l'enregistrement du contrat est delibere : il a son propre delai, son propre cout et sa
 * propre possibilite d'echec. Le meler au contrat rendrait impossible de suivre ce qui est en
 * attente aupres de l'administration · et c'est la que les dossiers s'enlisent.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "domiciliation_registration")
public class DomiciliationRegistration {

    public enum Status {
        PENDING, SUBMITTED, REGISTERED, REJECTED
    }

    public enum PaidBy {
        COMPANY, CLIENT
    }

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "registration_number", nullable = false, unique = true, length = 100)
    private String registrationNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "domiciliation_contract_id", nullable = false)
    private DomiciliationContract contract;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "submitted_by", length = 120)
    private String submittedBy;

    @Column(name = "administration_office")
    private String administrationOffice;

    /** La reference delivree par l'administration · celle qui figure sur l'attestation. */
    @Column(name = "administration_reference", length = 120)
    private String administrationReference;

    @Column(name = "registration_date")
    private LocalDate registrationDate;

    @Column(name = "stamp_duty_amount", precision = 19, scale = 4)
    private BigDecimal stampDutyAmount;

    @Column(name = "registration_fee_amount", precision = 19, scale = 4)
    private BigDecimal registrationFeeAmount;

    @Column(name = "total_duty_amount", precision = 19, scale = 4)
    private BigDecimal totalDutyAmount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "XAF";

    @Enumerated(EnumType.STRING)
    @Column(name = "paid_by", length = 20)
    private PaidBy paidBy;

    @Column(name = "rebilled", nullable = false)
    @Builder.Default
    private Boolean rebilled = Boolean.FALSE;

    @Column(name = "rebilled_document_code", length = 120)
    private String rebilledDocumentCode;

    @Column(name = "receipt_document_code", length = 120)
    private String receiptDocumentCode;

    @Column(name = "registered_document_code", length = 120)
    private String registeredDocumentCode;

    @Column(name = "expires_at")
    private LocalDate expiresAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
