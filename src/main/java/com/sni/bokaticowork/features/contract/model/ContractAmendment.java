package com.sni.bokaticowork.features.contract.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.contract.enums.AmendmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "contract_amendment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractAmendment {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 120)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", foreignKey = @ForeignKey(name = "fk_amendment_contract"))
    private Contract originalContract;

    @Column(name = "original_contract_code", nullable = false, length = 120)
    private String originalContractCode;

    @Column(name = "status", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    private AmendmentStatus status;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "proposed_by")
    private Long proposedBy;

    @Column(name = "proposed_at")
    private Instant proposedAt;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "draft_document_code", length = 120)
    private String draftDocumentCode;

    @Column(name = "signed_document_code", length = 120)
    private String signedDocumentCode;

    @Column(name = "signed_at")
    private Instant signedAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "review_comment")
    private String reviewComment;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = updatedAt == null ? now : updatedAt;
        proposedAt = proposedAt == null ? now : proposedAt;
        status = status == null ? AmendmentStatus.DRAFT : status;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
