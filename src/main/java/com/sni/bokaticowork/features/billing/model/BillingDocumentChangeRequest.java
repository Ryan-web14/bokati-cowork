package com.sni.bokaticowork.features.billing.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/** Demande de modification d'une facture non payee, soumise a approbation (lot B14). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "billing_document_change_request")
public class BillingDocumentChangeRequest {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "document_number", nullable = false, length = 60)
    private String documentNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private com.sni.bokaticowork.features.billing.enums.BillingChangeRequestStatus status;

    @Column(name = "reason", nullable = false, length = 1000)
    private String reason;

    /** Modifications proposees (meme forme que la mise a jour d'un document). */
    @Column(name = "changes_json", nullable = false, columnDefinition = "text")
    private String changesJson;

    @Column(name = "discount_override", length = 1000)
    private String discountOverride;

    @Column(name = "requested_by", nullable = false, length = 180)
    private String requestedBy;

    @Column(name = "requested_by_name")
    private String requestedByName;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "total_before", precision = 19, scale = 4)
    private BigDecimal totalBefore;

    @Column(name = "total_after", precision = 19, scale = 4)
    private BigDecimal totalAfter;

    @Column(name = "approval_number", length = 60)
    private String approvalNumber;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decided_by", length = 180)
    private String decidedBy;

    @Column(name = "decision_comment", length = 1000)
    private String decisionComment;

    /** NEW_VERSION (meme piece, nouvelle version) ou CORRECTIVE_INVOICE (rectificative, D8). */
    @Column(name = "result", length = 30)
    private String result;

    @Column(name = "result_document_number", length = 60)
    private String resultDocumentNumber;

    @Column(name = "version_number")
    private Integer versionNumber;
}
