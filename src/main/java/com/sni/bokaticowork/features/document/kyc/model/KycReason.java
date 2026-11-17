package com.sni.bokaticowork.features.document.kyc.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Lot KG7 : motif normalise de refus ou de correction (liste de reference tenue dans le backend).
 * {@code kind} : REJECTION (refus definitif), CORRECTION (demande de correction) ou BOTH.
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "kyc_reason")
public class KycReason {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 60)
    private String code;

    @Column(name = "label", nullable = false)
    private String label;

    @Builder.Default
    @Column(name = "kind", nullable = false, length = 20)
    private String kind = "BOTH";

    @Builder.Default
    @Column(name = "requires_text", nullable = false)
    private Boolean requiresText = Boolean.FALSE;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    /** Motif preenregistre : modifiable, pas supprimable (le desactiver). */
    @Builder.Default
    @Column(name = "system_reason", nullable = false)
    private Boolean systemReason = Boolean.FALSE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
