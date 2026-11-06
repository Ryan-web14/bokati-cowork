package com.sni.bokaticowork.features.marketplace.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Une ligne de commande · l'intitule et le prix sont figes a la validation, l'offre peut changer
 * ensuite sans toucher a la commande.
 */
@Entity
@Table(name = "marketplace_order_line")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceOrderLine {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @org.hibernate.annotations.Generated
    @Column(name = "short_id", insertable = false, updatable = false)
    private Long shortId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private MarketplaceOrder order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_id", nullable = false)
    private MarketplaceOffer offer;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "amount", precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "stock_reservation_code", length = 120)
    private String stockReservationCode;

    /** La sortie de stock a abouti · vide tant qu'elle reste a faire. */
    @Column(name = "stock_consumed_at")
    private Instant stockConsumedAt;

    /** Pourquoi le stock n'a pas pu bouger · l'administration est prevenue, la sortie est reessayee. */
    @Column(name = "stock_error", length = 500)
    private String stockError;

    @Column(name = "billable_number", length = 120)
    private String billableNumber;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
