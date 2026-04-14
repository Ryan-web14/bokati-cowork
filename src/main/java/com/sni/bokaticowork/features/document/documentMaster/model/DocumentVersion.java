package com.sni.bokaticowork.features.document.documentMaster.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentAntivirusStatus;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentVersionUploadStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "document_version",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_document_version_number", columnNames = {"document_id", "version_number"})
        })
public class DocumentVersion {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false, foreignKey = @ForeignKey(name = "fk_document_version_document"))
    private Document document;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "storage_provider", nullable = false)
    private String storageProvider;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "stored_file_name", nullable = false)
    private String storedFileName;

    @Column(name = "mime_type_declared")
    private String mimeTypeDeclared;

    @Column(name = "mime_type_detected", nullable = false)
    private String mimeTypeDetected;

    @Column(name = "file_extension")
    private String fileExtension;

    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "upload_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentVersionUploadStatus uploadStatus;

    @Column(name = "antivirus_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentAntivirusStatus antivirusStatus;

    @Column(name = "uploaded_by")
    private Long uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @Column(name = "is_current", nullable = false)
    private Boolean current;

    @PrePersist
    public void prePersist() {
        uploadedAt = uploadedAt == null ? Instant.now() : uploadedAt;
        current = current == null ? Boolean.FALSE : current;
    }
}
