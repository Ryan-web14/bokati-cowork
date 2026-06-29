package com.sni.bokaticowork.features.document.documentMaster.repository;

import com.sni.bokaticowork.features.document.documentMaster.model.DocumentWorkflow;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentWorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentWorkflowStepRepository extends JpaRepository<DocumentWorkflowStep, Long> {
    List<DocumentWorkflowStep> findAllByWorkflowOrderByStepOrderAsc(DocumentWorkflow workflow);
    Optional<DocumentWorkflowStep> findByWorkflowAndStepOrder(DocumentWorkflow workflow, Integer stepOrder);
}
