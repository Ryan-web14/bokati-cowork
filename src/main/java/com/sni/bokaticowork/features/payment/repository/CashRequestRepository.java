package com.sni.bokaticowork.features.payment.repository;

import com.sni.bokaticowork.features.payment.model.CashRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CashRequestRepository extends JpaRepository<CashRequest, Long> {

    Optional<CashRequest> findByRequestNumber(String requestNumber);

    @Query(nativeQuery = true,
            value = """
                    SELECT cr.*
                    FROM cash_request cr
                    JOIN cash_session cs ON cs.id = cr.cash_session_id
                    JOIN cash_register reg ON reg.id = cs.cash_register_id
                    WHERE (CAST(:sessionNumber AS VARCHAR) IS NULL OR cs.session_number = CAST(:sessionNumber AS VARCHAR))
                      AND (CAST(:registerCode AS VARCHAR) IS NULL OR reg.register_code = CAST(:registerCode AS VARCHAR))
                      AND (CAST(:requestType AS VARCHAR) IS NULL OR cr.request_type = CAST(:requestType AS VARCHAR))
                      AND (CAST(:status AS VARCHAR) IS NULL OR cr.status = CAST(:status AS VARCHAR))
                      AND (CAST(:requestedBy AS VARCHAR) IS NULL OR cr.requested_by = CAST(:requestedBy AS VARCHAR))
                      AND (
                          CAST(:searchText AS VARCHAR) IS NULL
                          OR cr.request_number ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR cs.session_number ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(cr.requested_by, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(cr.reason, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                      )
                    ORDER BY cr.requested_at DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM cash_request cr
                    JOIN cash_session cs ON cs.id = cr.cash_session_id
                    JOIN cash_register reg ON reg.id = cs.cash_register_id
                    WHERE (CAST(:sessionNumber AS VARCHAR) IS NULL OR cs.session_number = CAST(:sessionNumber AS VARCHAR))
                      AND (CAST(:registerCode AS VARCHAR) IS NULL OR reg.register_code = CAST(:registerCode AS VARCHAR))
                      AND (CAST(:requestType AS VARCHAR) IS NULL OR cr.request_type = CAST(:requestType AS VARCHAR))
                      AND (CAST(:status AS VARCHAR) IS NULL OR cr.status = CAST(:status AS VARCHAR))
                      AND (CAST(:requestedBy AS VARCHAR) IS NULL OR cr.requested_by = CAST(:requestedBy AS VARCHAR))
                      AND (
                          CAST(:searchText AS VARCHAR) IS NULL
                          OR cr.request_number ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR cs.session_number ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(cr.requested_by, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(cr.reason, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                      )
                    """)
    Page<CashRequest> search(@Param("registerCode") String registerCode,
                             @Param("sessionNumber") String sessionNumber,
                             @Param("requestType") String requestType,
                             @Param("status") String status,
                             @Param("requestedBy") String requestedBy,
                             @Param("searchText") String searchText,
                             Pageable pageable);
}
