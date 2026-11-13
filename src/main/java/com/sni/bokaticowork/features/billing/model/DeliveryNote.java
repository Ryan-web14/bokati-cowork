package com.sni.bokaticowork.features.billing.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Lot C4 : bon de livraison. Piece non fiscale tiree d'un devis accepte ou d'une facture ; elle
 * constate ce qui part chez le client et, si {@code billing.stock.issue-on} vaut DELIVERY, sort
 * le stock a la livraison. Statuts : DRAFT, DELIVERED, CANCELLED, INVOICED.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "billing_delivery_note")
public class DeliveryNote {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "note_number", nullable = false, unique = true, length = 40)
    private String noteNumber;

    @Column(name = "source_document_number", nullable = false, length = 80)
    private String sourceDocumentNumber;

    @Column(name = "source_document_type", nullable = false, length = 30)
    private String sourceDocumentType;

    @Column(name = "customer_type", length = 60)
    private String customerType;

    @Column(name = "customer_code", length = 120)
    private String customerCode;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "delivery_address", columnDefinition = "text")
    private String deliveryAddress;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "show_prices", nullable = false)
    @Builder.Default
    private Boolean showPrices = Boolean.FALSE;

    @Column(name = "stock_issued", nullable = false)
    @Builder.Default
    private Boolean stockIssued = Boolean.FALSE;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "delivered_by", length = 180)
    private String deliveredBy;

    @Column(name = "received_by", length = 255)
    private String receivedBy;

    @Column(name = "signed_copy_reference", length = 120)
    private String signedCopyReference;

    @Column(name = "invoice_number", length = 80)
    private String invoiceNumber;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by", length = 180)
    private String cancelledBy;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "created_by", length = 180)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
