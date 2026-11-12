package com.sni.bokaticowork.features.inventory.procurement.suggestion.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** Nature d'operation ou categorie de stock reliee a une categorie fournisseur. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inventory_supplier_category_ref_target",
        uniqueConstraints = @UniqueConstraint(columnNames = {"category_id", "target_kind", "target_code"}))
public class SupplierCategoryRefTarget {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    /** NATURE ou INVENTORY_CATEGORY. */
    @Enumerated(EnumType.STRING)
    @Column(name = "target_kind", nullable = false, length = 30)
    private SupplierCategoryKind targetKind;

    @Column(name = "target_code", nullable = false, length = 80)
    private String targetCode;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
