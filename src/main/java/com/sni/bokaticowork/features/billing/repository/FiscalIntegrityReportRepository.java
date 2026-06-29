package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.FiscalIntegrityReportLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FiscalIntegrityReportRepository extends JpaRepository<FiscalIntegrityReportLog, Long> {

    @Query(nativeQuery = true, value = """
            SELECT * FROM fiscal_integrity_report
            ORDER BY checked_at DESC
            LIMIT 1
            """)
    Optional<FiscalIntegrityReportLog> findLatest();
}
