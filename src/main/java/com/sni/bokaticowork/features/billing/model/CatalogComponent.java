package com.sni.bokaticowork.features.billing.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Lot C14 : composant d'un article compose (lot d'articles). Source SERVICE_CATALOG (prestation du
 * catalogue) ou INVENTORY_ITEM (article d'inventaire, dont le stock sort a la vente).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "billing_catalog_component")
public class CatalogComponent {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "parent_item_code", nullable = false, length = 80)
    private String parentItemCode;

    @Column(name = "component_code", nullable = false, length = 100)
    private String componentCode;

    @Column(name = "component_source", nullable = false, length = 20)
    private String componentSource;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;
}
