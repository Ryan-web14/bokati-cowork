package com.sni.bokaticowork.features.document.documentMaster.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "document_share_link")
public class DocumentShareLink {
    @Id @IdGeneration @Column(name = "id") private Long id;
    @Column(name = "token", nullable = false, unique = true, length = 100) private String token;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", foreignKey = @ForeignKey(name = "fk_share_link_document"))
    private Document document;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", foreignKey = @ForeignKey(name = "fk_share_link_folder"))
    private DocumentFolder folder;
    @Column(name = "created_by", nullable = false) private Long createdBy;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "password_hash", length = 255) private String passwordHash;
    @Builder.Default @Column(name = "allow_download", nullable = false) private Boolean allowDownload = Boolean.TRUE;
    @Column(name = "max_access_count") private Integer maxAccessCount;
    @Builder.Default @Column(name = "access_count", nullable = false) private Integer accessCount = 0;
    @Builder.Default @Column(name = "active", nullable = false) private Boolean active = Boolean.TRUE;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "last_accessed_at") private Instant lastAccessedAt;

    @PrePersist public void prePersist() { createdAt = createdAt == null ? Instant.now() : createdAt; }
}
