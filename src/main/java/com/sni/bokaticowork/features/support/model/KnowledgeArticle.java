package com.sni.bokaticowork.features.support.model;

import com.sni.bokaticowork.features.support.enums.TicketCategory;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "support_knowledge_article")
public class KnowledgeArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "article_code", nullable = false, unique = true, length = 40)
    private String articleCode;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "slug", nullable = false, unique = true, length = 255)
    private String slug;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 30)
    private TicketCategory category;

    @Column(name = "tags", length = 500)
    private String tags;

    @Column(name = "public_visible", nullable = false)
    @Builder.Default
    private Boolean publicVisible = Boolean.FALSE;

    @Column(name = "internal_only", nullable = false)
    @Builder.Default
    private Boolean internalOnly = Boolean.TRUE;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Long viewCount = 0L;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() { createdAt = updatedAt = Instant.now(); }

    @PreUpdate
    public void preUpdate() { updatedAt = Instant.now(); }
}
