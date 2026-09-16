package com.sni.bokaticowork.features.inventory.stock.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.inventory.stock.enums.InventoryPeriodStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Periode comptable de stock. Une fois close, plus aucun mouvement ne peut y etre date.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inventory_period", uniqueConstraints = {
        @UniqueConstraint(name = "uk_inventory_period_code", columnNames = "period_code")
})
public class InventoryPeriod {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    /** Code lisible, par exemple 2026-09. */
    @Column(name = "period_code", nullable = false, length = 40)
    private String periodCode;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private InventoryPeriodStatus status = InventoryPeriodStatus.OPEN;

    @Column(name = "closed_by", length = 120)
    private String closedBy;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "reopened_by", length = 120)
    private String reopenedBy;

    @Column(name = "reopened_at")
    private Instant reopenedAt;

    @Column(name = "reopen_reason", columnDefinition = "TEXT")
    private String reopenReason;

    /** Valeur totale du stock figee a la cloture. */
    @Column(name = "closing_stock_value")
    private Long closingStockValue;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = InventoryPeriodStatus.OPEN;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    /** Vrai si la date fournie tombe dans cette periode, bornes comprises. */
    public boolean covers(LocalDate date) {
        return date != null && !date.isBefore(startDate) && !date.isAfter(endDate);
    }
}
