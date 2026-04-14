package com.sni.bokaticowork.features.inventory.procurement.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "goods_receipt_line")
public class GoodsReceiptLine {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goods_receipt_id", nullable = false)
    private GoodsReceipt goodsReceipt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @Column(name = "received_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal receivedQuantity;

    @Column(name = "rejected_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal rejectedQuantity;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "quality_accepted", nullable = false)
    private Boolean qualityAccepted;

    @Column(name = "backorder_quantity", precision = 19, scale = 4)
    private BigDecimal backorderQuantity;

    @Column(name = "unit_cost")
    private Long unitCost;

    @Column(name = "lot_number", length = 120)
    private String lotNumber;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @PrePersist
    void prePersist() {
        if (rejectedQuantity == null) rejectedQuantity = BigDecimal.ZERO;
        if (qualityAccepted == null) qualityAccepted = true;
    }
}
