package com.sni.bokaticowork.features.support.repository;

import com.sni.bokaticowork.features.support.enums.TicketStatus;
import com.sni.bokaticowork.features.support.model.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    Optional<SupportTicket> findByTicketNumber(String ticketNumber);

    boolean existsByRelatedTypeAndRelatedCodeAndStatusIn(String relatedType, String relatedCode, List<TicketStatus> statuses);

    @Query(nativeQuery = true, value = """
            SELECT st.*
            FROM support_ticket st
            WHERE (CAST(:status AS TEXT) IS NULL OR st.status = CAST(:status AS TEXT))
              AND (CAST(:assignedTo AS BIGINT) IS NULL OR st.assigned_to = CAST(:assignedTo AS BIGINT))
              AND (CAST(:ownerType AS TEXT) IS NULL OR st.owner_type = CAST(:ownerType AS TEXT))
              AND (CAST(:ownerCode AS TEXT) IS NULL OR st.owner_code = CAST(:ownerCode AS TEXT))
              AND (CAST(:category AS TEXT) IS NULL OR st.category = CAST(:category AS TEXT))
              AND (CAST(:priority AS TEXT) IS NULL OR st.priority = CAST(:priority AS TEXT))
              AND (CAST(:relatedType AS TEXT) IS NULL OR st.related_type = CAST(:relatedType AS TEXT))
              AND (CAST(:relatedCode AS TEXT) IS NULL OR st.related_code = CAST(:relatedCode AS TEXT))
              AND (
                CAST(:overdueOnly AS BOOLEAN) IS NOT TRUE
                OR (
                  st.status IN ('OPEN','IN_PROGRESS')
                  AND (
                    (st.first_responded_at IS NULL AND st.first_response_due_at < CAST(:now AS TIMESTAMPTZ))
                    OR (st.resolved_at IS NULL AND st.resolution_due_at < CAST(:now AS TIMESTAMPTZ))
                  )
                )
              )
              AND (
                CAST(:searchText AS TEXT) IS NULL
                OR LOWER(st.title) LIKE LOWER('%' || CAST(:searchText AS TEXT) || '%')
                OR LOWER(COALESCE(st.description, '')) LIKE LOWER('%' || CAST(:searchText AS TEXT) || '%')
                OR st.ticket_number LIKE '%' || CAST(:searchText AS TEXT) || '%'
              )
            ORDER BY st.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM support_ticket st
            WHERE (CAST(:status AS TEXT) IS NULL OR st.status = CAST(:status AS TEXT))
              AND (CAST(:assignedTo AS BIGINT) IS NULL OR st.assigned_to = CAST(:assignedTo AS BIGINT))
              AND (CAST(:ownerType AS TEXT) IS NULL OR st.owner_type = CAST(:ownerType AS TEXT))
              AND (CAST(:ownerCode AS TEXT) IS NULL OR st.owner_code = CAST(:ownerCode AS TEXT))
              AND (CAST(:category AS TEXT) IS NULL OR st.category = CAST(:category AS TEXT))
              AND (CAST(:priority AS TEXT) IS NULL OR st.priority = CAST(:priority AS TEXT))
              AND (CAST(:relatedType AS TEXT) IS NULL OR st.related_type = CAST(:relatedType AS TEXT))
              AND (CAST(:relatedCode AS TEXT) IS NULL OR st.related_code = CAST(:relatedCode AS TEXT))
              AND (
                CAST(:overdueOnly AS BOOLEAN) IS NOT TRUE
                OR (
                  st.status IN ('OPEN','IN_PROGRESS')
                  AND (
                    (st.first_responded_at IS NULL AND st.first_response_due_at < CAST(:now AS TIMESTAMPTZ))
                    OR (st.resolved_at IS NULL AND st.resolution_due_at < CAST(:now AS TIMESTAMPTZ))
                  )
                )
              )
              AND (
                CAST(:searchText AS TEXT) IS NULL
                OR LOWER(st.title) LIKE LOWER('%' || CAST(:searchText AS TEXT) || '%')
                OR LOWER(COALESCE(st.description, '')) LIKE LOWER('%' || CAST(:searchText AS TEXT) || '%')
                OR st.ticket_number LIKE '%' || CAST(:searchText AS TEXT) || '%'
              )
            """)
    Page<SupportTicket> search(@Param("status") String status,
                               @Param("assignedTo") Long assignedTo,
                               @Param("ownerType") String ownerType,
                               @Param("ownerCode") String ownerCode,
                               @Param("category") String category,
                               @Param("priority") String priority,
                               @Param("relatedType") String relatedType,
                               @Param("relatedCode") String relatedCode,
                               @Param("overdueOnly") Boolean overdueOnly,
                               @Param("now") Instant now,
                               @Param("searchText") String searchText,
                               Pageable pageable);

    // ── SLA escalation worker ─────────────────────────────────────
    @Query("""
            SELECT t FROM SupportTicket t
            WHERE t.status IN ('OPEN','IN_PROGRESS')
              AND (
                (t.firstRespondedAt IS NULL AND t.firstResponseDueAt < :now)
                OR
                (t.resolvedAt IS NULL AND t.resolutionDueAt < :now)
              )
            """)
    List<SupportTicket> findSlaBreachCandidates(@Param("now") Instant now);

    // ── Auto-close worker ─────────────────────────────────────────
    @Query("""
            SELECT t FROM SupportTicket t
            WHERE t.status = 'RESOLVED'
              AND t.resolvedAt IS NOT NULL
              AND t.resolvedAt < :cutoff
              AND NOT EXISTS (
                SELECT m FROM TicketMessage m
                WHERE m.ticket = t
                  AND m.senderType = 'CLIENT'
                  AND m.createdAt > t.resolvedAt
              )
            """)
    List<SupportTicket> findAutoCloseCandidates(@Param("cutoff") Instant cutoff);

    // ── CSAT request worker ───────────────────────────────────────
    @Query("""
            SELECT t FROM SupportTicket t
            WHERE t.status = 'CLOSED'
              AND t.closedAt < :closedBefore
              AND t.csatEmailSentAt IS NULL
              AND t.contactEmail IS NOT NULL
            """)
    List<SupportTicket> findCsatRequestCandidates(@Param("closedBefore") Instant closedBefore);

    // ── Metrics (count queries · évite le findAll()) ──────────────
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.status IN ('OPEN','IN_PROGRESS')")
    long countOpen();

    @Query("""
            SELECT COUNT(t) FROM SupportTicket t
            WHERE t.firstRespondedAt IS NULL
              AND t.firstResponseDueAt IS NOT NULL
              AND t.firstResponseDueAt < :now
              AND t.status IN ('OPEN','IN_PROGRESS')
            """)
    long countOverdueFirstResponse(@Param("now") Instant now);

    @Query("""
            SELECT COUNT(t) FROM SupportTicket t
            WHERE t.resolvedAt IS NULL
              AND t.resolutionDueAt IS NOT NULL
              AND t.resolutionDueAt < :now
              AND t.status IN ('OPEN','IN_PROGRESS')
            """)
    long countOverdueResolution(@Param("now") Instant now);

    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.status IN ('RESOLVED','CLOSED')")
    long countResolved();

    // ── Analytics ─────────────────────────────────────────────────
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.createdAt >= :from AND t.createdAt <= :to")
    long countCreatedBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            SELECT COUNT(t) FROM SupportTicket t
            WHERE t.status IN ('RESOLVED','CLOSED')
              AND t.resolvedAt >= :from AND t.resolvedAt <= :to
            """)
    long countResolvedBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            SELECT COUNT(t) FROM SupportTicket t
            WHERE t.status IN ('OPEN','IN_PROGRESS')
              AND t.createdAt <= :to
            """)
    long countOpenAt(@Param("to") Instant to);

    @Query("""
            SELECT COUNT(t) FROM SupportTicket t
            WHERE t.firstRespondedAt IS NULL
              AND t.firstResponseDueAt < :now
              AND t.createdAt >= :from AND t.createdAt <= :to
            """)
    long countSlaBreachesBetween(@Param("now") Instant now,
                                 @Param("from") Instant from,
                                 @Param("to") Instant to);

    @Query(nativeQuery = true, value = """
            SELECT AVG(EXTRACT(EPOCH FROM (first_responded_at - created_at)) / 3600)
            FROM support_ticket
            WHERE first_responded_at IS NOT NULL
              AND created_at >= :from AND created_at <= :to
            """)
    Double avgFirstResponseHoursBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query(nativeQuery = true, value = """
            SELECT AVG(EXTRACT(EPOCH FROM (resolved_at - created_at)) / 3600)
            FROM support_ticket
            WHERE resolved_at IS NOT NULL
              AND created_at >= :from AND created_at <= :to
            """)
    Double avgResolutionHoursBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            SELECT t.category, COUNT(t) FROM SupportTicket t
            WHERE t.createdAt >= :from AND t.createdAt <= :to
            GROUP BY t.category
            """)
    List<Object[]> countByCategoryBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            SELECT t.priority, COUNT(t) FROM SupportTicket t
            WHERE t.createdAt >= :from AND t.createdAt <= :to
            GROUP BY t.priority
            """)
    List<Object[]> countByPriorityBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query(nativeQuery = true, value = """
            SELECT AVG(csat_score) FROM support_ticket
            WHERE csat_submitted_at IS NOT NULL
              AND created_at >= :from AND created_at <= :to
            """)
    Double avgCsatScoreBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            SELECT COUNT(t) FROM SupportTicket t
            WHERE t.csatSubmittedAt IS NOT NULL
              AND t.createdAt >= :from AND t.createdAt <= :to
            """)
    long countCsatResponsesBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            SELECT t.assignedTo, COUNT(t), COUNT(t.resolvedAt)
            FROM SupportTicket t
            WHERE t.assignedTo IS NOT NULL
              AND t.createdAt >= :from AND t.createdAt <= :to
            GROUP BY t.assignedTo
            """)
    List<Object[]> agentWorkloadBetween(@Param("from") Instant from, @Param("to") Instant to);

    // ── Client 360 / owner summary ────────────────────────────────
    @Query("""
            SELECT COUNT(t) FROM SupportTicket t
            WHERE t.ownerType = :ownerType AND t.ownerCode = :ownerCode
              AND t.status IN ('OPEN','IN_PROGRESS','WAITING_CLIENT')
            """)
    long countOpenByOwner(@Param("ownerType") String ownerType, @Param("ownerCode") String ownerCode);

    @Query("""
            SELECT COUNT(t) FROM SupportTicket t
            WHERE t.ownerType = :ownerType AND t.ownerCode = :ownerCode
              AND t.status IN ('OPEN','IN_PROGRESS')
              AND (
                (t.firstRespondedAt IS NULL AND t.firstResponseDueAt < :now)
                OR (t.resolvedAt IS NULL AND t.resolutionDueAt < :now)
              )
            """)
    long countSlaBreachesByOwner(@Param("ownerType") String ownerType,
                                 @Param("ownerCode") String ownerCode,
                                 @Param("now") Instant now);

    @Query(nativeQuery = true, value = """
            SELECT AVG(csat_score) FROM support_ticket
            WHERE owner_type = :ownerType AND owner_code = :ownerCode
              AND csat_submitted_at IS NOT NULL
            """)
    Double avgCsatScoreByOwner(@Param("ownerType") String ownerType, @Param("ownerCode") String ownerCode);

    List<SupportTicket> findTop5ByOwnerTypeAndOwnerCodeOrderByCreatedAtDesc(String ownerType, String ownerCode);

    // ── Analytics avancés (Phase 10) ───────────────────────────────
    long countByStatus(TicketStatus status);

    @Query("""
            SELECT t.assignedTo, COUNT(t) FROM SupportTicket t
            WHERE t.assignedTo IS NOT NULL AND t.status IN ('OPEN','IN_PROGRESS','WAITING_CLIENT')
            GROUP BY t.assignedTo
            """)
    List<Object[]> currentBacklogByAgent();

    @Query("""
            SELECT t.relatedType, COUNT(t) FROM SupportTicket t
            WHERE t.relatedType IS NOT NULL
              AND t.createdAt >= :from AND t.createdAt <= :to
            GROUP BY t.relatedType
            """)
    List<Object[]> volumeByRelatedTypeBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query(nativeQuery = true, value = """
            SELECT category, AVG(csat_score) FROM support_ticket
            WHERE csat_submitted_at IS NOT NULL
              AND created_at >= :from AND created_at <= :to
            GROUP BY category
            """)
    List<Object[]> csatByCategoryBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query(nativeQuery = true, value = """
            SELECT assigned_to, AVG(csat_score) FROM support_ticket
            WHERE csat_submitted_at IS NOT NULL AND assigned_to IS NOT NULL
              AND created_at >= :from AND created_at <= :to
            GROUP BY assigned_to
            """)
    List<Object[]> csatByAgentBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query(nativeQuery = true, value = """
            SELECT assigned_to,
                   COUNT(*) AS assigned,
                   COUNT(resolved_at) AS resolved,
                   AVG(EXTRACT(EPOCH FROM (first_responded_at - created_at)) / 3600) AS avg_first_response_hours,
                   AVG(EXTRACT(EPOCH FROM (resolved_at - created_at)) / 3600) AS avg_resolution_hours,
                   AVG(csat_score) AS avg_csat
            FROM support_ticket
            WHERE assigned_to IS NOT NULL
              AND created_at >= :from AND created_at <= :to
            GROUP BY assigned_to
            """)
    List<Object[]> agentPerformanceBetween(@Param("from") Instant from, @Param("to") Instant to);
}
