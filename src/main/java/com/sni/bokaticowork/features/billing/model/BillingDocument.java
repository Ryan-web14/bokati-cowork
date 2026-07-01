package com.sni.bokaticowork.features.billing.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "billing_document", indexes = {
        @Index(name = "idx_billing_document_number", columnList = "document_number"),
        @Index(name = "idx_billing_document_customer", columnList = "customer_type,customer_code"),
        @Index(name = "idx_billing_document_status", columnList = "document_type,status"),
        @Index(name = "idx_billing_document_source", columnList = "source_type,source_code")
})
public class BillingDocument {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "document_number", nullable = false, unique = true, length = 100)
    private String documentNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 40)
    private BillingDocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private BillingDocumentStatus status = BillingDocumentStatus.DRAFT;

    @Column(name = "customer_type", nullable = false, length = 60)
    private String customerType;

    @Column(name = "customer_code", nullable = false, length = 120)
    private String customerCode;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "customer_phone", length = 60)
    private String customerPhone;

    @org.hibernate.annotations.ColumnTransformer(write = "?::jsonb")
    @Column(name = "billing_address_json", columnDefinition = "jsonb")
    private String billingAddressJson;

    @Column(name = "source_type", length = 80)
    private String sourceType;

    @Column(name = "source_code", length = 120)
    private String sourceCode;

    @Column(name = "title")
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "terms", columnDefinition = "text")
    private String terms;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "subtotal_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal subtotalAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "taxable_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal taxableAmount = BigDecimal.ZERO;

    @Column(name = "vat_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal vatAmount = BigDecimal.ZERO;

    @Column(name = "additional_cent_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal additionalCentAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "paid_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "balance_due", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal balanceDue = BigDecimal.ZERO;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "customer_reference", length = 100)
    private String customerReference;

    @Column(name = "po_number", length = 100)
    private String poNumber;

    @Column(name = "project_code", length = 100)
    private String projectCode;

    @Column(name = "salesperson_code", length = 50)
    private String salespersonCode;

    @org.hibernate.annotations.ColumnTransformer(write = "?::jsonb")
    @Column(name = "delivery_address_json", columnDefinition = "jsonb")
    private String deliveryAddressJson;

    @Column(name = "language", length = 5)
    @Builder.Default
    private String language = "fr";

    @Column(name = "exchange_rate", precision = 19, scale = 6)
    private BigDecimal exchangeRate;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "payment_instructions", columnDefinition = "text")
    private String paymentInstructions;

    @org.hibernate.annotations.ColumnTransformer(write = "?::jsonb")
    @Column(name = "bank_details_json", columnDefinition = "jsonb")
    private String bankDetailsJson;

    @org.hibernate.annotations.ColumnTransformer(write = "?::jsonb")
    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private String metadataJson;

    @Column(name = "internal_notes", columnDefinition = "text")
    private String internalNotes;

    // ── Champs SEFC Phase 1 ────────────────────────────────────────────────────

    @Column(name = "locked", nullable = false)
    @Builder.Default
    private Boolean locked = false;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @Column(name = "fiscal_date")
    private LocalDate fiscalDate;

    /** Numéro fiscal SEFC définitif (FAC20260617-00001abc-BKTWK). Assigné à la validation. */
    @Column(name = "fiscal_number", unique = true, length = 80)
    private String fiscalNumber;

    // ── Chaînage cryptographique (Phase 2) ─────────────────────────────────────

    @Column(name = "previous_hash", length = 512)
    private String previousHash;

    @Column(name = "current_hash", length = 512)
    private String currentHash;

    @Column(name = "hash_algorithm", length = 40)
    private String hashAlgorithm;

    // ── Signature HMAC (Phase 2) ────────────────────────────────────────────────

    @Column(name = "fiscal_signature", length = 512)
    private String fiscalSignature;

    @Column(name = "signature_algorithm", length = 40)
    private String signatureAlgorithm;

    @Column(name = "signed_at")
    private Instant signedAt;

    // ── Snapshot vendeur ────────────────────────────────────────────────────────

    @Column(name = "seller_name", length = 255)
    private String sellerName;

    @Column(name = "seller_niu", length = 100)
    private String sellerNiu;

    @org.hibernate.annotations.ColumnTransformer(write = "?::jsonb")
    @Column(name = "seller_address_json", columnDefinition = "jsonb")
    private String sellerAddressJson;

    @Column(name = "seller_phone", length = 60)
    private String sellerPhone;

    @Column(name = "seller_email", length = 255)
    private String sellerEmail;

    // ── Identification fiscale client (optionnelle) ─────────────────────────────

    /** NIU client — renseigné uniquement si c'est une entité immatriculée. */
    @Column(name = "customer_niu", length = 100)
    private String customerNiu;

    /** ENTREPRISE ou PARTICULIER — toujours affiché sur le PDF. */
    @Column(name = "customer_category", length = 40)
    private String customerCategory;

    // ── Lien document-correctif ─────────────────────────────────────────────────

    /** Numéro du document d'origine corrigé par cet avoir ou cette facture rectificative. */
    @Column(name = "original_document_number", length = 100)
    private String originalDocumentNumber;

    /** Type du document d'origine (INVOICE, PROFORMA_INVOICE). */
    @Column(name = "original_document_type", length = 40)
    private String originalDocumentType;

    /** Motif de l'avoir ou de la rectification. */
    @Column(name = "credit_note_reason", columnDefinition = "TEXT")
    private String creditNoteReason;

    // ── Suppression logique ─────────────────────────────────────────────────────

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by", length = 120)
    private String deletedBy;

    @Column(name = "delete_reason", columnDefinition = "text")
    private String deleteReason;

    // ────────────────────────────────────────────────────────────────────────────

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
