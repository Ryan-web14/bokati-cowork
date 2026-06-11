package com.sni.bokaticowork.features.payment.repository;

import com.sni.bokaticowork.features.payment.model.CashSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CashSessionRepository extends JpaRepository<CashSession, Long> {

    @Query(nativeQuery = true, value = "SELECT * FROM cash_session WHERE session_number = :sessionNumber")
    Optional<CashSession> findBySessionNumber(@Param("sessionNumber") String sessionNumber);

    java.util.List<CashSession> findByOpenedByOrderByOpenedAtDesc(String openedBy);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM cash_session
            WHERE cash_register_id = :cashRegisterId
              AND (CAST(:fromDate AS TIMESTAMPTZ) IS NULL OR opened_at >= CAST(:fromDate AS TIMESTAMPTZ))
              AND (CAST(:toDate AS TIMESTAMPTZ) IS NULL OR opened_at <= CAST(:toDate AS TIMESTAMPTZ))
            ORDER BY opened_at DESC
            """)
    java.util.List<CashSession> findByRegisterAndPeriod(@Param("cashRegisterId") Long cashRegisterId,
                                                         @Param("fromDate") java.time.Instant fromDate,
                                                         @Param("toDate") java.time.Instant toDate);

    @Query(nativeQuery = true, value = "SELECT * FROM cash_session WHERE session_number = :sessionNumber")
    Optional<CashSession> findBySessionNumberForUpdate(@Param("sessionNumber") String sessionNumber);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM cash_session
            WHERE cash_register_id = :cashRegisterId
              AND status = CAST(:status AS VARCHAR)
            ORDER BY opened_at DESC
            LIMIT 1
            FOR UPDATE
            """)
    Optional<CashSession> findFirstByCashRegisterIdAndStatusForUpdate(@Param("cashRegisterId") Long cashRegisterId,
                                                                      @Param("status") String status);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM cash_session
            WHERE cash_register_id = :cashRegisterId
              AND status = CAST(:status AS VARCHAR)
            ORDER BY opened_at DESC
            LIMIT 1
            """)
    Optional<CashSession> findFirstByCashRegisterIdAndStatus(@Param("cashRegisterId") Long cashRegisterId,
                                                             @Param("status") String status);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM cash_session
            WHERE opened_by = CAST(:openedBy AS VARCHAR)
              AND status = 'OPEN'
            ORDER BY opened_at DESC
            LIMIT 1
            """)
    Optional<CashSession> findOpenSessionByOpenedBy(@Param("openedBy") String openedBy);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM cash_session
            WHERE cash_register_id = :cashRegisterId
              AND status IN ('OPEN', 'CLOSING_REVIEW')
            ORDER BY opened_at DESC
            LIMIT 1
            """)
    Optional<CashSession> findBlockingSessionByCashRegisterId(@Param("cashRegisterId") Long cashRegisterId);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM cash_session
            WHERE opened_by = CAST(:openedBy AS VARCHAR)
              AND status IN ('OPEN', 'CLOSING_REVIEW')
            ORDER BY opened_at DESC
            LIMIT 1
            """)
    Optional<CashSession> findBlockingSessionByOpenedBy(@Param("openedBy") String openedBy);

    @Query(nativeQuery = true,
            value = """
                    SELECT cs.*
                    FROM cash_session cs
                    JOIN cash_register cr ON cr.id = cs.cash_register_id
                    WHERE (CAST(:registerCode AS VARCHAR) IS NULL OR cr.register_code = CAST(:registerCode AS VARCHAR))
                      AND (CAST(:status AS VARCHAR) IS NULL OR cs.status = CAST(:status AS VARCHAR))
                      AND (CAST(:openedBy AS VARCHAR) IS NULL OR cs.opened_by = CAST(:openedBy AS VARCHAR))
                      AND (CAST(:closedBy AS VARCHAR) IS NULL OR cs.closed_by = CAST(:closedBy AS VARCHAR))
                      AND (
                          CAST(:searchText AS VARCHAR) IS NULL
                          OR cs.session_number ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR cr.register_code ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(cs.opened_by, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(cs.closed_by, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(cs.reviewed_by, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                      )
                    ORDER BY cs.opened_at DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM cash_session cs
                    JOIN cash_register cr ON cr.id = cs.cash_register_id
                    WHERE (CAST(:registerCode AS VARCHAR) IS NULL OR cr.register_code = CAST(:registerCode AS VARCHAR))
                      AND (CAST(:status AS VARCHAR) IS NULL OR cs.status = CAST(:status AS VARCHAR))
                      AND (CAST(:openedBy AS VARCHAR) IS NULL OR cs.opened_by = CAST(:openedBy AS VARCHAR))
                      AND (CAST(:closedBy AS VARCHAR) IS NULL OR cs.closed_by = CAST(:closedBy AS VARCHAR))
                      AND (
                          CAST(:searchText AS VARCHAR) IS NULL
                          OR cs.session_number ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR cr.register_code ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(cs.opened_by, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(cs.closed_by, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(cs.reviewed_by, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                      )
                    """)
    Page<CashSession> search(@Param("registerCode") String registerCode,
                             @Param("status") String status,
                             @Param("openedBy") String openedBy,
                             @Param("closedBy") String closedBy,
                             @Param("searchText") String searchText,
                             Pageable pageable);

    @Query(nativeQuery = true, value = """
            SELECT
                COUNT(*) AS register_count,
                COUNT(*) FILTER (WHERE cr.active = true) AS active_register_count,
                COUNT(cs.id) FILTER (WHERE cs.status = 'OPEN') AS open_session_count,
                COUNT(cs.id) FILTER (WHERE cs.status = 'CLOSING_REVIEW') AS review_session_count,
                COUNT(cs.id) FILTER (WHERE cs.status = 'CLOSED') AS closed_session_count,
                COALESCE(SUM(CASE WHEN cs.status = 'CLOSING_REVIEW' THEN cs.variance_amount ELSE 0 END), 0) AS pending_variance_amount
            FROM cash_register cr
            LEFT JOIN cash_session cs ON cs.cash_register_id = cr.id
                AND (CAST(:fromDate AS TIMESTAMPTZ) IS NULL OR cs.opened_at >= CAST(:fromDate AS TIMESTAMPTZ))
                AND (CAST(:toDate AS TIMESTAMPTZ) IS NULL OR cs.opened_at <= CAST(:toDate AS TIMESTAMPTZ))
            WHERE (CAST(:registerCode AS VARCHAR) IS NULL OR cr.register_code = CAST(:registerCode AS VARCHAR))
              AND (CAST(:businessEntityCode AS VARCHAR) IS NULL OR cr.business_entity_code ILIKE CONCAT('%', CAST(:businessEntityCode AS VARCHAR), '%'))
            """)
    Object[] overviewCounts(@Param("registerCode") String registerCode,
                            @Param("businessEntityCode") String businessEntityCode,
                            @Param("fromDate") java.time.Instant fromDate,
                            @Param("toDate") java.time.Instant toDate);
}
