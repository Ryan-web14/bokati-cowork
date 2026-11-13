package com.sni.bokaticowork.features.document.template.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Version d'un modele (lot D2) : {@code DRAFT} (un seul brouillon a la fois), {@code PUBLISHED}
 * (une seule par modele, celle qui sert a l'impression) ou {@code ARCHIVED}. Une version publiee
 * n'est jamais modifiee : on publie un nouveau brouillon, l'ancienne passe aux archives.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "document_template_version")
public class DocumentTemplateVersion {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "template_code", nullable = false, length = 80)
    private String templateCode;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "page_format", nullable = false, length = 20)
    private String pageFormat;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "copies_json", nullable = false, columnDefinition = "jsonb")
    private List<Map<String, Object>> copies;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "style_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> style;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "blocks_json", nullable = false, columnDefinition = "jsonb")
    private List<Map<String, Object>> blocks;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "created_by", length = 150)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "published_by", length = 150)
    private String publishedBy;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @PrePersist
    void onCreate() {
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }
}
