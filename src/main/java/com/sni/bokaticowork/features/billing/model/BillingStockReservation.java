package com.sni.bokaticowork.features.billing.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/** Reservation de stock d'une ligne de devis accepte (lot B7). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "billing_stock_reservation")
public class BillingStockReservation {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "document_number", nullable = false, length = 60)
    private String documentNumber;

    @Column(name = "line_order")
    private Integer lineOrder;

    @Column(name = "item_code", nullable = false, length = 80)
    private String itemCode;

    @Column(name = "location_code", nullable = false, length = 80)
    private String locationCode;

    @Column(name = "reservation_code", nullable = false, length = 80)
    private String reservationCode;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    /** ACTIVE, RELEASED ou CONSUMED. */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /** Piece qui a sorti le stock (facture). */
    @Column(name = "closed_by", length = 60)
    private String closedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
