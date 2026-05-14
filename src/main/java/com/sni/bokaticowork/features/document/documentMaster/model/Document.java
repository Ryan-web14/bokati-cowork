package com.sni.bokaticowork.features.document.documentMaster.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentCategory;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "document")
@SQLDelete(sql = "UPDATE document SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
public class Document {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "owner_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private DocumentOwnerType ownerType;

    @Column(name = "category", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private DocumentCategory category;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "document_type_id", foreignKey = @ForeignKey(name = "fk_document_type"), nullable = false)
    private DocumentType documentType;

    @Column(name = "type_code", length = 50)
    private String typeCode;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "file_name", length = 350)
    private String fileName;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Column(name = "current_version_number")
    private Integer currentVersionNumber;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentStatus status;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "uploaded_by")
    private Long uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder.Default
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        uploadedAt = uploadedAt == null ? now : uploadedAt;
        updatedAt = updatedAt == null ? now : updatedAt;
        currentVersionNumber = currentVersionNumber == null ? 0 : currentVersionNumber;
        deleted = deleted == null ? false : deleted;
        if (typeCode == null && documentType != null) {
            typeCode = documentType.getCode();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
        if (typeCode == null && documentType != null) {
            typeCode = documentType.getCode();
        }
    }
}
