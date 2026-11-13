package com.sni.bokaticowork.features.billing.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Prix date d'un article du catalogue pour un segment de client ; l'historique reste (lot B10). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "service_catalog_price")
public class ServiceCatalogPrice {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "item_code", nullable = false, length = 80)
    private String itemCode;

    /** ALL, MEMBER, COMPANY ou VISITOR. */
    @Column(name = "segment", nullable = false, length = 20)
    private String segment;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "created_by", length = 180)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
