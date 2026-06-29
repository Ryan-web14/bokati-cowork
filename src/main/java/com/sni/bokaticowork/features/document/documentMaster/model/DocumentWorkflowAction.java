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
@Table(name = "document_workflow_action")
public class DocumentWorkflowAction {
    @Id @IdGeneration @Column(name = "id") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instance_id", nullable = false, foreignKey = @ForeignKey(name = "fk_workflow_action_instance"))
    private DocumentWorkflowInstance instance;
    @Column(name = "step_order", nullable = false) private Integer stepOrder;
    @Column(name = "action", nullable = false, length = 20) private String action;
    @Column(name = "actor_id", nullable = false) private Long actorId;
    @Column(name = "acted_at", nullable = false) private Instant actedAt;
    @Column(name = "comment", columnDefinition = "TEXT") private String comment;

    @PrePersist public void prePersist() { actedAt = actedAt == null ? Instant.now() : actedAt; }
}
