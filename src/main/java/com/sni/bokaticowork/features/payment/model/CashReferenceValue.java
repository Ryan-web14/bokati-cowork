package com.sni.bokaticowork.features.payment.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;

/**
 * Valeur d'une liste preenregistree (motifs, banques, types de piece...).
 *
 * <p>La valeur {@code OTHER} (« Autre, preciser ») exige un texte ; la desactiver retire le texte
 * libre de la liste.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "cash_reference_value")
public class CashReferenceValue {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "list_code", nullable = false, length = 60)
    private String listCode;

    @Column(name = "code", nullable = false, length = 60)
    private String code;

    @Column(name = "label", nullable = false, length = 255)
    private String label;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Builder.Default
    @Column(name = "requires_text", nullable = false)
    private Boolean requiresText = Boolean.FALSE;

    @Builder.Default
    @Column(name = "system_value", nullable = false)
    private Boolean systemValue = Boolean.FALSE;

    @Builder.Default
    @Column(name = "usage_count", nullable = false)
    private Long usageCount = 0L;

    @Column(name = "updated_by", length = 180)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
