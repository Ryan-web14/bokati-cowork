package com.sni.bokaticowork.features.inventory.procurement.suggestion.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** Rattachement d'un fournisseur d'inventaire a une categorie. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inventory_supplier_category",
        uniqueConstraints = @UniqueConstraint(columnNames = {"supplier_id", "category_kind", "category_code"}))
public class SupplierCategoryLink {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_kind", nullable = false, length = 30)
    private SupplierCategoryKind categoryKind;

    @Column(name = "category_code", nullable = false, length = 80)
    private String categoryCode;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
