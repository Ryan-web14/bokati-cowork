package com.sni.bokaticowork.features.inventory.procurement.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.inventory.procurement.enums.PurchaseApprovalLevel;
import com.sni.bokaticowork.features.inventory.procurement.enums.PurchaseOrderStatus;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryLocation;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inventory_purchase_order")
public class PurchaseOrder {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "order_code", nullable = false, length = 100, unique = true)
    private String orderCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private InventoryLocation location;

    @Column(name = "source_request_code", length = 100)
    private String sourceRequestCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private PurchaseOrderStatus status;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "ordered_by", length = 120)
    private String orderedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_level", nullable = false, length = 40)
    private PurchaseApprovalLevel approvalLevel;

    @Column(name = "approved_by", length = 120)
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "total_amount")
    private Long totalAmount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PurchaseOrderLine> lines = new ArrayList<>();

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = PurchaseOrderStatus.DRAFT;
        if (approvalLevel == null) approvalLevel = PurchaseApprovalLevel.NONE;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
