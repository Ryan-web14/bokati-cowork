package com.sni.bokaticowork.features.document.retention.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import com.sni.bokaticowork.features.document.retention.enums.DocumentRetentionAction;
import com.sni.bokaticowork.features.document.retention.enums.DocumentRetentionReference;
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
@Table(name = "document_retention_policy")
public class DocumentRetentionPolicy {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "space", length = 50)
    private DocumentSpace space;

    @Column(name = "document_type_code", length = 100)
    private String documentTypeCode;

    @Column(name = "retention_days", nullable = false)
    private Integer retentionDays;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "retention_reference", nullable = false, length = 30)
    private DocumentRetentionReference retentionReference = DocumentRetentionReference.UPLOAD_DATE;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private DocumentRetentionAction action = DocumentRetentionAction.ARCHIVE;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "created_by")
    private Long createdBy;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (retentionReference == null) retentionReference = DocumentRetentionReference.UPLOAD_DATE;
        if (action == null) action = DocumentRetentionAction.ARCHIVE;
        if (active == null) active = Boolean.TRUE;
    }
}
