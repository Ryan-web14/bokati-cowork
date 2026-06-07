package com.sni.bokaticowork.features.payment.repository;

import com.sni.bokaticowork.features.payment.model.CashMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CashMovementRepository extends JpaRepository<CashMovement, Long> {

    boolean existsByReferenceTypeAndReferenceCode(String referenceType, String referenceCode);

    Optional<CashMovement> findByMovementNumber(String movementNumber);

    java.util.List<CashMovement> findByCashSession_IdOrderByCreatedAtAsc(Long cashSessionId);

    java.util.List<CashMovement> findByCreatedByOrderByCreatedAtDesc(String createdBy);

    @Query(nativeQuery = true, value = """
            SELECT
                COALESCE(SUM(CASE WHEN cm.movement_type = 'PAYMENT' THEN cm.amount ELSE 0 END), 0) AS total_payments,
                COALESCE(SUM(CASE WHEN cm.movement_type = 'REFUND' THEN cm.amount ELSE 0 END), 0) AS total_refunds,
                COALESCE(SUM(CASE WHEN cm.movement_type IN ('CASH_IN', 'TRANSFER_IN') THEN cm.amount ELSE 0 END), 0) AS total_cash_in,
                COALESCE(SUM(CASE WHEN cm.movement_type IN ('CASH_OUT', 'SAFE_DEPOSIT', 'TRANSFER_OUT') THEN cm.amount ELSE 0 END), 0) AS total_cash_out,
                COALESCE(SUM(CASE WHEN cm.movement_type = 'ADJUSTMENT' THEN cm.amount ELSE 0 END), 0) AS total_adjustments
            FROM cash_movement cm
            WHERE cm.cash_session_id = :sessionId
            """)
    Object[] sessionTotals(@Param("sessionId") Long sessionId);

    @Query(nativeQuery = true,
            value = """
                    SELECT cm.*
                    FROM cash_movement cm
                    JOIN cash_session cs ON cs.id = cm.cash_session_id
                    JOIN cash_register cr ON cr.id = cs.cash_register_id
                    WHERE (CAST(:sessionNumber AS VARCHAR) IS NULL OR cs.session_number = CAST(:sessionNumber AS VARCHAR))
                      AND (CAST(:registerCode AS VARCHAR) IS NULL OR cr.register_code = CAST(:registerCode AS VARCHAR))
                      AND (CAST(:movementType AS VARCHAR) IS NULL OR cm.movement_type = CAST(:movementType AS VARCHAR))
                      AND (CAST(:documentType AS VARCHAR) IS NULL OR cm.document_type = CAST(:documentType AS VARCHAR))
                      AND (CAST(:documentNumber AS VARCHAR) IS NULL OR cm.document_number = CAST(:documentNumber AS VARCHAR))
                      AND (CAST(:flowCategory AS VARCHAR) IS NULL OR cm.flow_category = CAST(:flowCategory AS VARCHAR))
                      AND (CAST(:referenceType AS VARCHAR) IS NULL OR cm.reference_type = CAST(:referenceType AS VARCHAR))
                      AND (CAST(:referenceCode AS VARCHAR) IS NULL OR cm.reference_code = CAST(:referenceCode AS VARCHAR))
                      AND (CAST(:counterpartyCode AS VARCHAR) IS NULL OR cm.counterparty_code = CAST(:counterpartyCode AS VARCHAR))
                      AND (CAST(:counterpartyName AS VARCHAR) IS NULL OR cm.counterparty_name ILIKE CONCAT('%', CAST(:counterpartyName AS VARCHAR), '%'))
                      AND (CAST(:createdBy AS VARCHAR) IS NULL OR cm.created_by = CAST(:createdBy AS VARCHAR))
                      AND (CAST(:fromDate AS TIMESTAMPTZ) IS NULL OR cm.created_at >= CAST(:fromDate AS TIMESTAMPTZ))
                      AND (CAST(:toDate AS TIMESTAMPTZ) IS NULL OR cm.created_at <= CAST(:toDate AS TIMESTAMPTZ))
                      AND (
                          CAST(:searchText AS VARCHAR) IS NULL
                          OR cm.movement_number ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(cm.document_number, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(cm.flow_category, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(cm.reference_code, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(cm.counterparty_code, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(cm.counterparty_name, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(cm.created_by, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(cm.reason, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR cs.session_number ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR cr.register_code ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                      )
                    ORDER BY cm.created_at DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM cash_movement cm
                    JOIN cash_session cs ON cs.id = cm.cash_session_id
                    JOIN cash_register cr ON cr.id = cs.cash_register_id
                    WHERE (CAST(:sessionNumber AS VARCHAR) IS NULL OR cs.session_number = CAST(:sessionNumber AS VARCHAR))
                      AND (CAST(:registerCode AS VARCHAR) IS NULL OR cr.register_code = CAST(:registerCode AS VARCHAR))
                      AND (CAST(:movementType AS VARCHAR) IS NULL OR cm.movement_type = CAST(:movementType AS VARCHAR))
                      AND (CAST(:documentType AS VARCHAR) IS NULL OR cm.document_type = CAST(:documentType AS VARCHAR))
                      AND (CAST(:documentNumber AS VARCHAR) IS NULL OR cm.document_number = CAST(:documentNumber AS VARCHAR))
                      AND (CAST(:flowCategory AS VARCHAR) IS NULL OR cm.flow_category = CAST(:flowCategory AS VARCHAR))
                      AND (CAST(:referenceType AS VARCHAR) IS NULL OR cm.reference_type = CAST(:referenceType AS VARCHAR))
                      AND (CAST(:referenceCode AS VARCHAR) IS NULL OR cm.reference_code = CAST(:referenceCode AS VARCHAR))
                      AND (CAST(:counterpartyCode AS VARCHAR) IS NULL OR cm.counterparty_code = CAST(:counterpartyCode AS VARCHAR))
                      AND (CAST(:counterpartyName AS VARCHAR) IS NULL OR cm.counterparty_name ILIKE CONCAT('%', CAST(:counterpartyName AS VARCHAR), '%'))
                      AND (CAST(:createdBy AS VARCHAR) IS NULL OR cm.created_by = CAST(:createdBy AS VARCHAR))
                      AND (CAST(:fromDate AS TIMESTAMPTZ) IS NULL OR cm.created_at >= CAST(:fromDate AS TIMESTAMPTZ))
                      AND (CAST(:toDate AS TIMESTAMPTZ) IS NULL OR cm.created_at <= CAST(:toDate AS TIMESTAMPTZ))
                      AND (
                          CAST(:searchText AS VARCHAR) IS NULL
                          OR cm.movement_number ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(cm.document_number, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(cm.flow_category, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(cm.reference_code, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(cm.counterparty_code, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(cm.counterparty_name, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(cm.created_by, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(cm.reason, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR cs.session_number ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR cr.register_code ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                      )
                    """)
    Page<CashMovement> search(@Param("registerCode") String registerCode,
                              @Param("sessionNumber") String sessionNumber,
                              @Param("movementType") String movementType,
                              @Param("documentType") String documentType,
                              @Param("documentNumber") String documentNumber,
                              @Param("flowCategory") String flowCategory,
                              @Param("referenceType") String referenceType,
                              @Param("referenceCode") String referenceCode,
                              @Param("counterpartyCode") String counterpartyCode,
                              @Param("counterpartyName") String counterpartyName,
                              @Param("createdBy") String createdBy,
                              @Param("fromDate") java.time.Instant fromDate,
                              @Param("toDate") java.time.Instant toDate,
                              @Param("searchText") String searchText,
                              Pageable pageable);

    @Query(nativeQuery = true, value = """
            SELECT
                COUNT(*) AS movement_count,
                COALESCE(SUM(CASE WHEN cm.movement_type = 'OPENING_FLOAT' THEN cm.amount ELSE 0 END), 0) AS opening_float_amount,
                COALESCE(SUM(CASE WHEN cm.movement_type = 'PAYMENT' THEN cm.amount ELSE 0 END), 0) AS total_payments,
                COALESCE(SUM(CASE WHEN cm.movement_type = 'REFUND' THEN cm.amount ELSE 0 END), 0) AS total_refunds,
                COALESCE(SUM(CASE WHEN cm.movement_type IN ('CASH_IN', 'TRANSFER_IN') THEN cm.amount ELSE 0 END), 0) AS total_cash_in,
                COALESCE(SUM(CASE WHEN cm.movement_type IN ('CASH_OUT', 'SAFE_DEPOSIT', 'TRANSFER_OUT') THEN cm.amount ELSE 0 END), 0) AS total_cash_out,
                COALESCE(SUM(CASE WHEN cm.movement_type = 'ADJUSTMENT' THEN cm.amount ELSE 0 END), 0) AS total_adjustments
            FROM cash_movement cm
            JOIN cash_session cs ON cs.id = cm.cash_session_id
            JOIN cash_register cr ON cr.id = cs.cash_register_id
            WHERE (CAST(:registerCode AS VARCHAR) IS NULL OR cr.register_code = CAST(:registerCode AS VARCHAR))
              AND (CAST(:businessEntityCode AS VARCHAR) IS NULL OR cr.business_entity_code ILIKE CONCAT('%', CAST(:businessEntityCode AS VARCHAR), '%'))
              AND (CAST(:fromDate AS TIMESTAMPTZ) IS NULL OR cm.created_at >= CAST(:fromDate AS TIMESTAMPTZ))
              AND (CAST(:toDate AS TIMESTAMPTZ) IS NULL OR cm.created_at <= CAST(:toDate AS TIMESTAMPTZ))
            """)
    Object[] overviewAmounts(@Param("registerCode") String registerCode,
                             @Param("businessEntityCode") String businessEntityCode,
                             @Param("fromDate") java.time.Instant fromDate,
                             @Param("toDate") java.time.Instant toDate);

    @Query(nativeQuery = true, value = """
            SELECT
                cr.register_code,
                cr.name,
                cr.business_entity_code,
                cr.device_code,
                COUNT(DISTINCT cs.id) FILTER (WHERE cs.status = 'OPEN') AS open_session_count,
                COUNT(DISTINCT cs.id) FILTER (WHERE cs.status = 'CLOSING_REVIEW') AS review_session_count,
                COUNT(cm.id) AS movement_count,
                COALESCE(SUM(CASE WHEN cm.movement_type = 'PAYMENT' THEN cm.amount ELSE 0 END), 0) AS total_payments,
                COALESCE(SUM(CASE WHEN cm.movement_type = 'REFUND' THEN cm.amount ELSE 0 END), 0) AS total_refunds,
                COALESCE(SUM(CASE WHEN cm.movement_type IN ('CASH_IN', 'TRANSFER_IN') THEN cm.amount ELSE 0 END), 0) AS total_cash_in,
                COALESCE(SUM(CASE WHEN cm.movement_type IN ('CASH_OUT', 'SAFE_DEPOSIT', 'TRANSFER_OUT') THEN cm.amount ELSE 0 END), 0) AS total_cash_out,
                COALESCE(SUM(CASE WHEN cm.movement_type = 'ADJUSTMENT' THEN cm.amount ELSE 0 END), 0) AS total_adjustments
            FROM cash_register cr
            LEFT JOIN cash_session cs ON cs.cash_register_id = cr.id
            LEFT JOIN cash_movement cm ON cm.cash_session_id = cs.id
                AND (CAST(:fromDate AS TIMESTAMPTZ) IS NULL OR cm.created_at >= CAST(:fromDate AS TIMESTAMPTZ))
                AND (CAST(:toDate AS TIMESTAMPTZ) IS NULL OR cm.created_at <= CAST(:toDate AS TIMESTAMPTZ))
            WHERE (CAST(:businessEntityCode AS VARCHAR) IS NULL OR cr.business_entity_code ILIKE CONCAT('%', CAST(:businessEntityCode AS VARCHAR), '%'))
            GROUP BY cr.register_code, cr.name, cr.business_entity_code, cr.device_code
            ORDER BY cr.name ASC, cr.register_code ASC
            """)
    java.util.List<Object[]> registerMetrics(@Param("businessEntityCode") String businessEntityCode,
                                             @Param("fromDate") java.time.Instant fromDate,
                                             @Param("toDate") java.time.Instant toDate);
}
