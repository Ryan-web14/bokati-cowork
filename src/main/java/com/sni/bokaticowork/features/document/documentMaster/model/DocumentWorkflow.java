package com.sni.bokaticowork.features.document.documentMaster.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "document_workflow")
public class DocumentWorkflow {
    @Id @IdGeneration @Column(name = "id") private Long id;
    @Column(name = "code", nullable = false, unique = true, length = 100) private String code;
    @Column(name = "name", nullable = false, length = 300) private String name;
    @Column(name = "description", columnDefinition = "TEXT") private String description;
    @Builder.Default @Column(name = "workflow_type", nullable = false, length = 30) private String workflowType = "SIMPLE";
    @Column(name = "space", length = 50) @Enumerated(EnumType.STRING) private DocumentSpace space;
    @Column(name = "document_type_code", length = 100) private String documentTypeCode;
    @Builder.Default @Column(name = "active", nullable = false) private Boolean active = Boolean.TRUE;
    @Column(name = "created_by") private Long createdBy;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @PrePersist public void prePersist() { Instant now = Instant.now(); createdAt = createdAt == null ? now : createdAt; updatedAt = updatedAt == null ? now : updatedAt; }
    @PreUpdate public void preUpdate() { updatedAt = Instant.now(); }
}
