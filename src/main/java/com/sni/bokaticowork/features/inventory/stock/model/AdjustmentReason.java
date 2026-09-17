package com.sni.bokaticowork.features.inventory.stock.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Motif d'ajustement parametrable, avec son compte comptable.
 *
 * <p>Complete le texte libre porte jusqu'ici par {@code StockMovement.reason} : un ajustement sans
 * motif codifie ne peut etre ni impute comptablement, ni analyse par cause.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inventory_adjustment_reason", uniqueConstraints = {
        @UniqueConstraint(name = "uk_adjustment_reason_code", columnNames = "reason_code")
})
public class AdjustmentReason {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "reason_code", nullable = false, length = 40)
    private String reasonCode;

    @Column(name = "label", nullable = false, length = 160)
    private String label;

    /** Compte de contrepartie utilise pour l'ecriture de stock. */
    @Column(name = "counterpart_account", nullable = false, length = 40)
    private String counterpartAccount;

    /** Vrai lorsque le motif ne s'applique qu'aux ajustements negatifs. */
    @Builder.Default
    @Column(name = "negative_only", nullable = false)
    private Boolean negativeOnly = Boolean.FALSE;

    /** Vrai lorsque le motif ne s'applique qu'aux ajustements positifs. */
    @Builder.Default
    @Column(name = "positive_only", nullable = false)
    private Boolean positiveOnly = Boolean.FALSE;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        applyDefaults();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
        applyDefaults();
    }

    private void applyDefaults() {
        if (negativeOnly == null) negativeOnly = Boolean.FALSE;
        if (positiveOnly == null) positiveOnly = Boolean.FALSE;
        if (active == null) active = Boolean.TRUE;
    }

    /** Vrai si ce motif peut etre utilise pour un ajustement du sens fourni. */
    public boolean appliesTo(boolean positive) {
        if (Boolean.TRUE.equals(positiveOnly)) {
            return positive;
        }
        if (Boolean.TRUE.equals(negativeOnly)) {
            return !positive;
        }
        return true;
    }
}
