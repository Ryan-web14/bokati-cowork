package com.sni.bokaticowork.features.payment.repository;

import com.sni.bokaticowork.features.payment.model.CashAnomalyFlag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CashAnomalyFlagRepository extends JpaRepository<CashAnomalyFlag, Long> {

    Optional<CashAnomalyFlag> findByFlagNumber(String flagNumber);

    boolean existsByCashSession_IdAndAnomalyType(Long cashSessionId, com.sni.bokaticowork.features.payment.enums.CashAnomalyType anomalyType);

    @Query(nativeQuery = true,
            value = """
                    SELECT caf.*
                    FROM cash_anomaly_flag caf
                    LEFT JOIN cash_session cs ON cs.id = caf.cash_session_id
                    LEFT JOIN cash_register reg ON reg.id = cs.cash_register_id
                    WHERE (CAST(:registerCode AS VARCHAR) IS NULL OR reg.register_code = CAST(:registerCode AS VARCHAR))
                      AND (CAST(:sessionNumber AS VARCHAR) IS NULL OR cs.session_number = CAST(:sessionNumber AS VARCHAR))
                      AND (CAST(:severity AS VARCHAR) IS NULL OR caf.severity = CAST(:severity AS VARCHAR))
                      AND (CAST(:status AS VARCHAR) IS NULL OR caf.status = CAST(:status AS VARCHAR))
                      AND (CAST(:anomalyType AS VARCHAR) IS NULL OR caf.anomaly_type = CAST(:anomalyType AS VARCHAR))
                    ORDER BY caf.detected_at DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM cash_anomaly_flag caf
                    LEFT JOIN cash_session cs ON cs.id = caf.cash_session_id
                    LEFT JOIN cash_register reg ON reg.id = cs.cash_register_id
                    WHERE (CAST(:registerCode AS VARCHAR) IS NULL OR reg.register_code = CAST(:registerCode AS VARCHAR))
                      AND (CAST(:sessionNumber AS VARCHAR) IS NULL OR cs.session_number = CAST(:sessionNumber AS VARCHAR))
                      AND (CAST(:severity AS VARCHAR) IS NULL OR caf.severity = CAST(:severity AS VARCHAR))
                      AND (CAST(:status AS VARCHAR) IS NULL OR caf.status = CAST(:status AS VARCHAR))
                      AND (CAST(:anomalyType AS VARCHAR) IS NULL OR caf.anomaly_type = CAST(:anomalyType AS VARCHAR))
                    """)
    Page<CashAnomalyFlag> search(@Param("registerCode") String registerCode,
                                 @Param("sessionNumber") String sessionNumber,
                                 @Param("severity") String severity,
                                 @Param("status") String status,
                                 @Param("anomalyType") String anomalyType,
                                 Pageable pageable);

    @Query(nativeQuery = true, value = """
            SELECT COALESCE(AVG(cs.variance_amount), 0) AS avg_variance,
                   COALESCE(STDDEV_SAMP(cs.variance_amount), 0) AS stddev_variance,
                   COUNT(*) AS session_count
            FROM cash_session cs
            WHERE cs.opened_by = CAST(:cashierCode AS VARCHAR)
              AND cs.status = 'CLOSED'
              AND cs.variance_amount IS NOT NULL
            """)
    Object[] cashierVarianceStats(@Param("cashierCode") String cashierCode);
}
