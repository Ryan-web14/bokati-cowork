package com.sni.bokaticowork.features.billing.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Lot C0 : famille (sans parent) ou sous-famille (avec la famille parente) du catalogue. Le code
 * est stable et c'est lui que porte l'article ; le libelle se renomme librement.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "billing_catalog_category")
public class CatalogCategory {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "label", nullable = false, length = 150)
    private String label;

    /** Code de la famille parente ; nul pour une famille. */
    @Column(name = "parent_code", length = 100)
    private String parentCode;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    /** Compte de produit propose aux articles de la famille (facultatif). */
    @Column(name = "revenue_account", length = 20)
    private String revenueAccount;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    /** Ajoutee automatiquement a l'enregistrement d'un article (mode souple). */
    @Column(name = "created_from_item", nullable = false)
    @Builder.Default
    private Boolean createdFromItem = Boolean.FALSE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public boolean isFamily() {
        return parentCode == null;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
