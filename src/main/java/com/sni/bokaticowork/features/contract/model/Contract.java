package com.sni.bokaticowork.features.contract.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.contract.enums.ContractRenewalType;
import com.sni.bokaticowork.features.contract.enums.ContractStatus;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "contract_record")
@SQLDelete(sql = "UPDATE contract_record SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
public class Contract {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "contract_code", nullable = false, unique = true, length = 120)
    private String contractCode;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "template_code", nullable = false, length = 120)
    private String templateCode;

    @Column(name = "owner_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private DocumentOwnerType ownerType;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "owner_code", nullable = false, length = 120)
    private String ownerCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", foreignKey = @ForeignKey(name = "fk_contract_business"))
    private BusinessEntity business;

    @Column(name = "status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private ContractStatus status;

    @Column(name = "renewal_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private ContractRenewalType renewalType;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "signed_at")
    private Instant signedAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "terminated_at")
    private Instant terminatedAt;

    @Column(name = "termination_reason")
    private String terminationReason;

    @Column(name = "suspension_reason")
    private String suspensionReason;

    @Column(name = "draft_document_code")
    private String draftDocumentCode;

    @Column(name = "signed_document_code")
    private String signedDocumentCode;

    @Column(name = "renewed_from_code", length = 120)
    private String renewedFromCode;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "review_comment")
    private String reviewComment;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder.Default
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = updatedAt == null ? now : updatedAt;
        status = status == null ? ContractStatus.DRAFT : status;
        renewalType = renewalType == null ? ContractRenewalType.NONE : renewalType;
        deleted = deleted == null ? false : deleted;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
