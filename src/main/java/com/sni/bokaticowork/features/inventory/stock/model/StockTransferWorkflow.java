package com.sni.bokaticowork.features.inventory.stock.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.stock.enums.StockTransferWorkflowStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stock_transfer_workflow")
public class StockTransferWorkflow {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "transfer_code", nullable = false, unique = true, length = 100)
    private String transferCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_location_id", nullable = false)
    private InventoryLocation fromLocation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_location_id", nullable = false)
    private InventoryLocation toLocation;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private StockTransferWorkflowStatus status;

    @Column(name = "requested_by", length = 120)
    private String requestedBy;

    @Column(name = "approved_by", length = 120)
    private String approvedBy;

    @Column(name = "shipped_by", length = 120)
    private String shippedBy;

    @Column(name = "received_by", length = 120)
    private String receivedBy;

    @Column(name = "movement_code", length = 120)
    private String movementCode;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "shipped_at")
    private Instant shippedAt;

    @Column(name = "received_at")
    private Instant receivedAt;

    @PrePersist
    void prePersist() {
        if (status == null) status = StockTransferWorkflowStatus.REQUESTED;
        if (requestedAt == null) requestedAt = Instant.now();
    }
}
