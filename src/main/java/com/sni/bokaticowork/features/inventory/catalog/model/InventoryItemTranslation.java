package com.sni.bokaticowork.features.inventory.catalog.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Libelle d'un article dans une langue donnee.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inventory_item_translation", uniqueConstraints = {
        @UniqueConstraint(name = "uk_inventory_translation_item_lang", columnNames = {"item_id", "language_code"})
})
public class InventoryItemTranslation {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    /** Code langue sur deux lettres, en minuscules. */
    @Column(name = "language_code", nullable = false, length = 8)
    private String languageCode;

    @Column(name = "name", nullable = false, length = 220)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
