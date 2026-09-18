package com.sni.bokaticowork.features.payment.compliance.repository;

import com.sni.bokaticowork.features.payment.compliance.model.ComplianceCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComplianceCaseRepository extends JpaRepository<ComplianceCase, Long> {

    Optional<ComplianceCase> findByCaseNumber(String caseNumber);

    /** Le dossier ouvert d'un sujet · un seul a la fois, les signalements suivants s'y ajoutent. */
    Optional<ComplianceCase> findFirstBySubjectTypeAndSubjectCodeAndStatusInOrderByCreatedAtDesc(
            String subjectType, String subjectCode, Collection<ComplianceCase.Status> statuses);

    Page<ComplianceCase> findByStatusInOrderByPriorityDescDueAtAsc(Collection<ComplianceCase.Status> statuses, Pageable pageable);

    Page<ComplianceCase> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<ComplianceCase> findByAssignedToAndStatusInOrderByDueAtAsc(String assignedTo, Collection<ComplianceCase.Status> statuses, Pageable pageable);

    List<ComplianceCase> findBySubjectTypeAndSubjectCodeOrderByCreatedAtDesc(String subjectType, String subjectCode);

    long countByStatusIn(Collection<ComplianceCase.Status> statuses);

    @Query("SELECT COUNT(c) FROM ComplianceCase c WHERE c.status IN :statuses AND c.dueAt < :now")
    long countOverdue(@Param("statuses") Collection<ComplianceCase.Status> statuses, @Param("now") Instant now);

    @Query("SELECT c FROM ComplianceCase c WHERE c.status IN :statuses AND c.dueAt < :now ORDER BY c.dueAt ASC")
    List<ComplianceCase> findOverdue(@Param("statuses") Collection<ComplianceCase.Status> statuses, @Param("now") Instant now);

    @Query(nativeQuery = true, value = """
            SELECT priority, COUNT(*)
            FROM compliance_case
            WHERE status IN ('OPEN', 'IN_REVIEW', 'ESCALATED')
            GROUP BY priority
            """)
    List<Object[]> countOpenByPriority();

    @Query(nativeQuery = true, value = """
            SELECT
              SUM(CASE WHEN created_at > now() - INTERVAL '1 day' THEN 1 ELSE 0 END),
              SUM(CASE WHEN created_at <= now() - INTERVAL '1 day' AND created_at > now() - INTERVAL '7 days' THEN 1 ELSE 0 END),
              SUM(CASE WHEN created_at <= now() - INTERVAL '7 days' AND created_at > now() - INTERVAL '30 days' THEN 1 ELSE 0 END),
              SUM(CASE WHEN created_at <= now() - INTERVAL '30 days' THEN 1 ELSE 0 END)
            FROM compliance_case
            WHERE status IN ('OPEN', 'IN_REVIEW', 'ESCALATED')
            """)
    Object[] openByAge();
}
