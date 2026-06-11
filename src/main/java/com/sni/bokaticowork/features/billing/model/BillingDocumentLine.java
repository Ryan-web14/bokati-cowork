package com.sni.bokaticowork.features.billing.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.billing.enums.BillingLineType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "billing_document_line")
public class BillingDocumentLine {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false, foreignKey = @ForeignKey(name = "fk_billing_line_document"))
    private BillingDocument document;

    @Column(name = "line_order", nullable = false)
    private Integer lineOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "line_type", nullable = false, length = 40)
    private BillingLineType lineType;

    @Column(name = "item_code", length = 120)
    private String itemCode;

    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "detailed_description", columnDefinition = "text")
    private String detailedDescription;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "discount_rate", precision = 9, scale = 4)
    private BigDecimal discountRate;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal discountAmount;

    @Column(name = "taxable", nullable = false)
    private Boolean taxable;

    @Column(name = "tax_included", nullable = false)
    @Builder.Default
    private Boolean taxIncluded = Boolean.FALSE;

    @Column(name = "vat_rate", precision = 9, scale = 4)
    private BigDecimal vatRate;

    @Column(name = "additional_cent_rate", precision = 9, scale = 4)
    private BigDecimal additionalCentRate;

    @Column(name = "subtotal_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal subtotalAmount;

    @Column(name = "taxable_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal taxableAmount;

    @Column(name = "vat_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal vatAmount;

    @Column(name = "additional_cent_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal additionalCentAmount;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "unit", length = 30)
    private String unit;

    @Column(name = "external_reference", length = 100)
    private String externalReference;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "optional", nullable = false)
    @Builder.Default
    private Boolean optional = Boolean.FALSE;

    @Column(name = "source_type", length = 80)
    private String sourceType;

    @Column(name = "source_code", length = 120)
    private String sourceCode;

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
