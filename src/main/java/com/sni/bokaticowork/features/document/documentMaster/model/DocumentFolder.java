package com.sni.bokaticowork.features.document.documentMaster.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "document_folder")
@SQLDelete(sql = "UPDATE document_folder SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
public class DocumentFolder {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, length = 100, unique = true)
    private String code;

    @Column(name = "name", nullable = false, length = 300)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", foreignKey = @ForeignKey(name = "fk_document_folder_parent"))
    private DocumentFolder parent;

    @Builder.Default
    @Column(name = "space", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private DocumentSpace space = DocumentSpace.GENERIC;

    @Column(name = "owner_type", length = 20)
    @Enumerated(EnumType.STRING)
    private DocumentOwnerType ownerType;

    @Column(name = "owner_id")
    private Long ownerId;

    @Builder.Default
    @Column(name = "path", nullable = false, length = 2000)
    private String path = "/";

    @Builder.Default
    @Column(name = "depth", nullable = false)
    private Integer depth = 0;

    @Builder.Default
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "color", length = 7)
    private String color;

    @Column(name = "icon", length = 50)
    private String icon;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder.Default
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    /** Mis a la corbeille de la GED : restaurable avec les autres elements du meme lot. */
    @Column(name = "trashed_at")
    private Instant trashedAt;

    @Column(name = "trashed_by")
    private Long trashedBy;

    @Column(name = "trash_batch", length = 40)
    private String trashBatch;

    /**
     * Cle d'un dossier du plan de classement automatique ({@code owner:MEMBER:12:KYC}). Un dossier
     * systeme ne se renomme, ne se deplace ni ne se supprime : le classement le retrouve par elle.
     */
    @Column(name = "system_key", length = 120)
    private String systemKey;

    /**
     * Acces restreint : le dossier et son contenu ne sont visibles que des personnes et roles qui y
     * ont un droit. Voir {@code GedAccessService}.
     */
    @Column(name = "restricted", nullable = false)
    @Builder.Default
    private Boolean restricted = Boolean.FALSE;

    @Column(name = "restricted_at")
    private java.time.Instant restrictedAt;

    @Column(name = "restricted_by")
    private Long restrictedBy;

    /** Quota d'espace du dossier, sous-dossiers compris, en octets ; vide : pas de limite. */
    @Column(name = "quota_bytes")
    private Long quotaBytes;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = updatedAt == null ? now : updatedAt;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
