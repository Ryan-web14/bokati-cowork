package com.sni.bokaticowork.features.document.documentMaster.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "document_workflow_step")
public class DocumentWorkflowStep {
    @Id @IdGeneration @Column(name = "id") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false, foreignKey = @ForeignKey(name = "fk_workflow_step_workflow"))
    private DocumentWorkflow workflow;
    @Column(name = "step_order", nullable = false) private Integer stepOrder;
    @Column(name = "step_name", nullable = false, length = 200) private String stepName;
    @Column(name = "approver_type", nullable = false, length = 30) private String approverType;
    @Column(name = "approver_value", nullable = false, length = 200) private String approverValue;
    @Builder.Default @Column(name = "required", nullable = false) private Boolean required = Boolean.TRUE;
    @Column(name = "auto_approve_days") private Integer autoApproveDays;
}
