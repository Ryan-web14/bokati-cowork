package com.sni.bokaticowork.features.inventory.catalog.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Changement de revision d'un article, par exemple une evolution de specification technique.
 *
 * <p>Permet de savoir quelle revision etait en vigueur au moment d'une reception ou d'une sortie.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inventory_item_revision_history")
public class InventoryItemRevisionHistory {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @Column(name = "previous_revision", length = 40)
    private String previousRevision;

    @Column(name = "new_revision", nullable = false, length = 40)
    private String newRevision;

    @Column(name = "changed_by", length = 120)
    private String changedBy;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "document_code", length = 120)
    private String documentCode;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @PrePersist
    void prePersist() {
        if (changedAt == null) changedAt = Instant.now();
    }
}
