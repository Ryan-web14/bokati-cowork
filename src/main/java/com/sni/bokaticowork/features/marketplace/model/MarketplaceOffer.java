package com.sni.bokaticowork.features.marketplace.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
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
 * Ce que le portail vend · un article de stock, sorti d'un emplacement, ou une prestation du
 * catalogue, sans stock.
 *
 * <p>Sans prix propre, l'offre suit celui de l'article ou de la prestation.</p>
 */
@Entity
@Table(name = "marketplace_offer")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceOffer {

    public enum Kind { PRODUCT, SERVICE }

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @org.hibernate.annotations.Generated
    @Column(name = "short_id", insertable = false, updatable = false)
    private Long shortId;

    @Column(name = "offer_code", nullable = false, length = 60)
    private String offerCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private Kind kind;

    @Column(name = "inventory_item_code", length = 120)
    private String inventoryItemCode;

    @Column(name = "location_code", length = 120)
    private String locationCode;

    @Column(name = "service_item_code", length = 120)
    private String serviceItemCode;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "category", length = 120)
    private String category;

    @Column(name = "price", precision = 19, scale = 4)
    private BigDecimal price;

    @Builder.Default
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "XAF";

    @Column(name = "max_per_order")
    private Integer maxPerOrder;

    @Builder.Default
    @Column(name = "published", nullable = false)
    private Boolean published = Boolean.FALSE;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "created_by", length = 150)
    private String createdBy;

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
