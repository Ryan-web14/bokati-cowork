package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.FiscalAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FiscalAuditLogRepository extends JpaRepository<FiscalAuditLog, Long> {

    @Query(nativeQuery = true, value = """
            SELECT * FROM fiscal_audit_log
            WHERE entity_type = :entityType AND entity_id = :entityId
            ORDER BY created_at ASC
            """)
    List<FiscalAuditLog> findByEntity(@Param("entityType") String entityType,
                                      @Param("entityId") String entityId);
}
