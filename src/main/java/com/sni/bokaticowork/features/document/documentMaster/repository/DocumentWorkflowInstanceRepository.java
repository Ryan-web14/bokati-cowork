package com.sni.bokaticowork.features.document.documentMaster.repository;

import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentWorkflowInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentWorkflowInstanceRepository extends JpaRepository<DocumentWorkflowInstance, Long> {
    Optional<DocumentWorkflowInstance> findByDocumentAndStatusIn(Document document, List<String> statuses);
    Optional<DocumentWorkflowInstance> findByDocumentAndStatusNot(Document document, String status);
    List<DocumentWorkflowInstance> findAllByStatusInOrderByStartedAtAsc(List<String> statuses);

    @Query("""
        SELECT wi FROM DocumentWorkflowInstance wi
        JOIN DocumentWorkflowStep ws ON ws.workflow = wi.workflow AND ws.stepOrder = wi.currentStep
        WHERE wi.status IN ('PENDING','IN_PROGRESS')
        AND (ws.approverType = 'USER' AND ws.approverValue = :email
             OR ws.approverType = 'ROLE' AND ws.approverValue IN :roles)
        ORDER BY wi.startedAt ASC
    """)
    List<DocumentWorkflowInstance> findPendingForApprover(@Param("email") String email, @Param("roles") List<String> roles);
}
