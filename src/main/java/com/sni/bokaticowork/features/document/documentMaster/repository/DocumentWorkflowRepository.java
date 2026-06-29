package com.sni.bokaticowork.features.document.documentMaster.repository;

import com.sni.bokaticowork.features.document.documentMaster.model.DocumentWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentWorkflowRepository extends JpaRepository<DocumentWorkflow, Long> {
    Optional<DocumentWorkflow> findByCode(String code);
    List<DocumentWorkflow> findAllByActiveTrueOrderByNameAsc();
    List<DocumentWorkflow> findAllByOrderByNameAsc();
}
