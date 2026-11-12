package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Version de budget d'un exercice. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_budget")
public class AccBudget {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "fiscal_year_code", nullable = false)
    private String fiscalYearCode;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "label", nullable = false)
    private String label;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private String status = "DRAFT";

    @Column(name = "reason")
    private String reason;

    @Column(name = "submitted_by")
    private String submittedBy;

    @Column(name = "consolidated_by")
    private String consolidatedBy;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_at")
    private java.time.Instant approvedAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private java.time.Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = java.time.Instant.now();
        }
    }
}
