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
@Table(name = "document_workflow_instance")
public class DocumentWorkflowInstance {
    @Id @IdGeneration @Column(name = "id") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false, foreignKey = @ForeignKey(name = "fk_workflow_instance_workflow"))
    private DocumentWorkflow workflow;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false, foreignKey = @ForeignKey(name = "fk_workflow_instance_document"))
    private Document document;
    @Builder.Default @Column(name = "current_step", nullable = false) private Integer currentStep = 1;
    @Builder.Default @Column(name = "status", nullable = false, length = 30) private String status = "PENDING";
    @Column(name = "started_at", nullable = false) private Instant startedAt;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "started_by") private Long startedBy;

    @PrePersist public void prePersist() { startedAt = startedAt == null ? Instant.now() : startedAt; }
}
