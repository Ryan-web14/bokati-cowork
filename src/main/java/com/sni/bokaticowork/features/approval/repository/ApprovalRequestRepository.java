package com.sni.bokaticowork.features.approval.repository;

import com.sni.bokaticowork.features.approval.enums.ApprovalStatus;
import com.sni.bokaticowork.features.approval.model.ApprovalRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {

    Optional<ApprovalRequest> findByNumber(String number);

    List<ApprovalRequest> findByCenterAndRequestTypeAndSubjectRefAndStatus(String center, String requestType, String subjectRef,
                                                                          ApprovalStatus status);

    List<ApprovalRequest> findByCenterAndRequestTypeAndSubjectRefOrderBySubmittedAtDesc(String center, String requestType, String subjectRef);

    List<ApprovalRequest> findByStatusOrderBySubmittedAtAsc(ApprovalStatus status);

    List<ApprovalRequest> findByCenterAndStatusOrderBySubmittedAtAsc(String center, ApprovalStatus status);

    List<ApprovalRequest> findByCenterAndRequesterIgnoreCaseOrderBySubmittedAtDesc(String center, String requester);

    /**
     * Historique d'un centre. {@code viewer} vide : tout (droit APPROVAL:MANAGE) ; sinon, les
     * demandes ou il est demandeur, decideur attendu ou auteur d'une decision.
     */
    @Query("""
            SELECT r FROM ApprovalRequest r
            WHERE r.center = :center
              AND r.status IN :statuses
              AND r.submittedAt >= :from AND r.submittedAt < :to
              AND (:requestType = '' OR r.requestType = :requestType)
              AND (:requester = '' OR lower(r.requester) = lower(:requester))
              AND (:approver = '' OR EXISTS (SELECT 1 FROM ApprovalEvent e WHERE e.requestId = r.id
                       AND lower(e.actor) = lower(:approver) AND e.event <> com.sni.bokaticowork.features.approval.enums.ApprovalEventType.SUBMITTED))
              AND (:viewer = '' OR lower(r.requester) = lower(:viewer)
                   OR EXISTS (SELECT 1 FROM ApprovalEvent e2 WHERE e2.requestId = r.id AND lower(e2.actor) = lower(:viewer))
                   OR EXISTS (SELECT 1 FROM ApprovalStep s WHERE s.requestId = r.id AND lower(s.expectedApprover) = lower(:viewer)))
            ORDER BY r.submittedAt DESC
            """)
    Page<ApprovalRequest> history(@Param("center") String center,
                                  @Param("statuses") Collection<ApprovalStatus> statuses,
                                  @Param("from") Instant from,
                                  @Param("to") Instant to,
                                  @Param("requestType") String requestType,
                                  @Param("requester") String requester,
                                  @Param("approver") String approver,
                                  @Param("viewer") String viewer,
                                  Pageable pageable);
}
