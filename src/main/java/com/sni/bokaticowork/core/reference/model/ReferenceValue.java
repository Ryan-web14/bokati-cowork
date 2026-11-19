package com.sni.bokaticowork.core.reference.model;

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
 * Valeur d'une liste de l'administrateur (socle des listes de choix, lot L0) : motifs, banques,
 * types de piece, canaux... Promue depuis les valeurs de reference de la caisse (table
 * {@code cash_reference_value} renommee {@code reference_value}).
 *
 * <p>La valeur {@code OTHER} (« Autre, preciser ») exige un texte. Une valeur n'est jamais
 * supprimee : desactivee, elle garde son libelle pour les enregistrements qui la citent.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "reference_value")
public class ReferenceValue {
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

    /** Valeur livree avec l'application : desactivable, jamais renommee en code. */
    @Builder.Default
    @Column(name = "system_value", nullable = false)
    private Boolean systemValue = Boolean.FALSE;

    /** Ajoutee d'elle-meme par une saisie libre dans une liste souple, a relire par l'administrateur. */
    @Builder.Default
    @Column(name = "auto_added", nullable = false)
    private Boolean autoAdded = Boolean.FALSE;

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
