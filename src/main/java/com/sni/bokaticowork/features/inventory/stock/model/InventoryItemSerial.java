package com.sni.bokaticowork.features.inventory.stock.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.stock.enums.InventorySerialStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inventory_item_serial",
        uniqueConstraints = @UniqueConstraint(columnNames = {"item_id", "serial_number"}))
public class InventoryItemSerial {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private InventoryLocation location;

    @Column(name = "serial_number", nullable = false, length = 200)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private InventorySerialStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private StockLot lot;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @PrePersist
    void prePersist() {
        if (receivedAt == null) receivedAt = Instant.now();
        if (status == null) status = InventorySerialStatus.AVAILABLE;
    }
}
