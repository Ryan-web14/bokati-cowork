package com.sni.bokaticowork.features.inventory.catalog.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryBarcodeType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Un code-barres parmi ceux que porte un article.
 *
 * <p>Un meme article en porte souvent plusieurs : le code du fournisseur, le code interne, et un
 * code par conditionnement. La quantite representee permet de savoir qu'un scan de carton vaut
 * vingt-quatre unites.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inventory_barcode", uniqueConstraints = {
        @UniqueConstraint(name = "uk_inventory_barcode_value", columnNames = "barcode_value")
})
public class InventoryBarcode {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @Enumerated(EnumType.STRING)
    @Column(name = "barcode_type", nullable = false, length = 30)
    private InventoryBarcodeType barcodeType;

    @Column(name = "barcode_value", nullable = false, length = 120)
    private String barcodeValue;

    /** Unite representee par ce code, lorsqu'il designe un conditionnement. */
    @Column(name = "unit_code", length = 80)
    private String unitCode;

    /** Nombre d'unites de base representees par un scan de ce code. */
    @Builder.Default
    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity = BigDecimal.ONE;

    /** Code principal de l'article, celui qui sert a l'etiquetage. */
    @Builder.Default
    @Column(name = "is_primary", nullable = false)
    private Boolean primaryCode = Boolean.FALSE;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        if (quantity == null) quantity = BigDecimal.ONE;
        if (primaryCode == null) primaryCode = Boolean.FALSE;
        if (active == null) active = Boolean.TRUE;
    }
}
