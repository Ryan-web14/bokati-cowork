package com.sni.bokaticowork.features.document.documentMaster.repository;

import com.sni.bokaticowork.features.document.documentMaster.model.DocumentWorkflowAction;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentWorkflowInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentWorkflowActionRepository extends JpaRepository<DocumentWorkflowAction, Long> {
    List<DocumentWorkflowAction> findAllByInstanceOrderByActedAtAsc(DocumentWorkflowInstance instance);
}
