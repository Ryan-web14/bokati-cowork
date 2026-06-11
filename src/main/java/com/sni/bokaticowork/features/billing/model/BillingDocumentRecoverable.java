package com.sni.bokaticowork.features.billing.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "billing_document_recoverable")
public class BillingDocumentRecoverable {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "recoverable_number", nullable = false, unique = true, length = 100)
    private String recoverableNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false, foreignKey = @ForeignKey(name = "fk_recoverable_document"))
    private BillingDocument document;

    @Column(name = "item_description", nullable = false, length = 500)
    private String itemDescription;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "source_type", length = 80)
    private String sourceType;

    @Column(name = "source_code", length = 120)
    private String sourceCode;

    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "recovered_at")
    private Instant recoveredAt;

    @Column(name = "recovered_by", length = 120)
    private String recoveredBy;

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
