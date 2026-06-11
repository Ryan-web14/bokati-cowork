package com.sni.bokaticowork.features.crm.repository;

import com.sni.bokaticowork.features.crm.enums.LeadStage;
import com.sni.bokaticowork.features.crm.model.Lead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LeadRepository extends JpaRepository<Lead, Long> {

    Optional<Lead> findByLeadNumber(String leadNumber);

    @Query(nativeQuery = true, value = """
            SELECT l.*
            FROM crm_lead l
            WHERE (CAST(:stage AS TEXT) IS NULL OR l.stage = CAST(:stage AS TEXT))
              AND (CAST(:assignedTo AS BIGINT) IS NULL OR l.assigned_to = CAST(:assignedTo AS BIGINT))
              AND (CAST(:source AS TEXT) IS NULL OR l.source = CAST(:source AS TEXT))
              AND (
                CAST(:searchText AS TEXT) IS NULL
                OR LOWER(l.full_name) LIKE LOWER('%' || CAST(:searchText AS TEXT) || '%')
                OR LOWER(COALESCE(l.email, '')) LIKE LOWER('%' || CAST(:searchText AS TEXT) || '%')
                OR LOWER(COALESCE(l.company, '')) LIKE LOWER('%' || CAST(:searchText AS TEXT) || '%')
                OR l.lead_number LIKE '%' || CAST(:searchText AS TEXT) || '%'
              )
            ORDER BY l.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM crm_lead l
            WHERE (CAST(:stage AS TEXT) IS NULL OR l.stage = CAST(:stage AS TEXT))
              AND (CAST(:assignedTo AS BIGINT) IS NULL OR l.assigned_to = CAST(:assignedTo AS BIGINT))
              AND (CAST(:source AS TEXT) IS NULL OR l.source = CAST(:source AS TEXT))
              AND (
                CAST(:searchText AS TEXT) IS NULL
                OR LOWER(l.full_name) LIKE LOWER('%' || CAST(:searchText AS TEXT) || '%')
                OR LOWER(COALESCE(l.email, '')) LIKE LOWER('%' || CAST(:searchText AS TEXT) || '%')
                OR LOWER(COALESCE(l.company, '')) LIKE LOWER('%' || CAST(:searchText AS TEXT) || '%')
                OR l.lead_number LIKE '%' || CAST(:searchText AS TEXT) || '%'
              )
            """)
    Page<Lead> search(@Param("stage") String stage,
                      @Param("assignedTo") Long assignedTo,
                      @Param("source") String source,
                      @Param("searchText") String searchText,
                      Pageable pageable);

    @Query("SELECT l FROM Lead l WHERE l.stage = :stage")
    List<Lead> findAllByStage(@Param("stage") LeadStage stage);

    @Query(nativeQuery = true, value = """
            SELECT l.*
            FROM crm_lead l
            WHERE l.stage = CAST(:stage AS TEXT)
              AND (CAST(:assignedTo AS BIGINT) IS NULL OR l.assigned_to = CAST(:assignedTo AS BIGINT))
              AND (CAST(:source AS TEXT) IS NULL OR l.source = CAST(:source AS TEXT))
              AND (CAST(:interest AS TEXT) IS NULL OR l.interest = CAST(:interest AS TEXT))
              AND (
                CAST(:searchText AS TEXT) IS NULL
                OR LOWER(l.full_name) LIKE LOWER('%' || CAST(:searchText AS TEXT) || '%')
                OR LOWER(COALESCE(l.email, '')) LIKE LOWER('%' || CAST(:searchText AS TEXT) || '%')
                OR LOWER(COALESCE(l.company, '')) LIKE LOWER('%' || CAST(:searchText AS TEXT) || '%')
                OR l.lead_number LIKE '%' || CAST(:searchText AS TEXT) || '%'
              )
            ORDER BY COALESCE(l.last_activity_at, l.created_at) DESC
            """)
    List<Lead> findByStageFiltered(@Param("stage") String stage,
                                   @Param("assignedTo") Long assignedTo,
                                   @Param("source") String source,
                                   @Param("interest") String interest,
                                   @Param("searchText") String searchText);

    // ── Dormant worker ────────────────────────────────────────────
    @Query("""
            SELECT l FROM Lead l
            WHERE l.stage NOT IN ('WON','LOST')
              AND (l.lastActivityAt IS NULL OR l.lastActivityAt < :before)
              AND l.dormantAlertSentAt IS NULL
              AND l.assignedTo IS NOT NULL
            """)
    List<Lead> findDormantCandidates(@Param("before") Instant before);

    // ── Métriques (count queries) ─────────────────────────────────
    @Query("SELECT COUNT(l) FROM Lead l WHERE l.stage = :stage")
    long countByStage(@Param("stage") LeadStage stage);

    @Query("""
            SELECT COALESCE(SUM(l.estimatedAmount), 0)
            FROM Lead l
            WHERE l.stage NOT IN ('WON','LOST')
              AND l.estimatedAmount IS NOT NULL
            """)
    BigDecimal sumActivePipelineValue();

    // ── Analytics ─────────────────────────────────────────────────
    @Query("SELECT COUNT(l) FROM Lead l WHERE l.createdAt >= :from AND l.createdAt <= :to")
    long countCreatedBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COUNT(l) FROM Lead l WHERE l.stage = 'WON' AND l.updatedAt >= :from AND l.updatedAt <= :to")
    long countWonBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COUNT(l) FROM Lead l WHERE l.stage = 'LOST' AND l.updatedAt >= :from AND l.updatedAt <= :to")
    long countLostBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT l.source, COUNT(l) FROM Lead l WHERE l.createdAt >= :from AND l.createdAt <= :to GROUP BY l.source")
    List<Object[]> countBySourceBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT l.stage, COUNT(l) FROM Lead l WHERE l.createdAt >= :from AND l.createdAt <= :to GROUP BY l.stage")
    List<Object[]> countByStageBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query(nativeQuery = true, value = """
            SELECT AVG(EXTRACT(EPOCH FROM (updated_at - created_at)) / 86400)
            FROM crm_lead
            WHERE stage = 'WON' AND updated_at >= :from AND updated_at <= :to
            """)
    Double avgDaysToWinBetween(@Param("from") Instant from, @Param("to") Instant to);
}
