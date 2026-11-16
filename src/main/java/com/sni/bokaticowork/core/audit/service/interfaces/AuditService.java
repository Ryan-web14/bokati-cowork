package com.sni.bokaticowork.core.audit.service.interfaces;


import com.sni.bokaticowork.core.audit.dto.request.AuditLogFilter;
import com.sni.bokaticowork.core.audit.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditService {
    void save(AuditLog auditLog);

    Page<AuditLog> list(AuditLogFilter filter, Pageable pageable);

    /** Export CSV, du plus recent au plus ancien, borne a {@code maxRows} lignes. */
    AuditExport export(AuditLogFilter filter, int maxRows);

    /** Contenu CSV et indication d'une coupure a la limite. */
    record AuditExport(String csv, int rows, boolean truncated) {
    }
}
