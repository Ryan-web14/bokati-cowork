package com.sni.bokaticowork.features.inventory.procurement.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.inventory.procurement.enums.GoodsReceiptStatus;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryLocation;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "goods_receipt")
public class GoodsReceipt {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "receipt_code", nullable = false, length = 100, unique = true)
    private String receiptCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id")
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private InventoryLocation location;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private GoodsReceiptStatus status;

    @Column(name = "received_by", length = 120)
    private String receivedBy;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "invoice_document_code", length = 120)
    private String invoiceDocumentCode;

    @Column(name = "delivery_note_document_code", length = 120)
    private String deliveryNoteDocumentCode;

    @Column(name = "proof_document_code", length = 120)
    private String proofDocumentCode;

    @OneToMany(mappedBy = "goodsReceipt", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GoodsReceiptLine> lines = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (status == null) status = GoodsReceiptStatus.DRAFT;
        if (receivedAt == null) receivedAt = Instant.now();
    }
}
