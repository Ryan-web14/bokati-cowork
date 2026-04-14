package com.sni.bokaticowork.core.audit.service.interfaces;


import com.sni.bokaticowork.core.audit.enums.AuditStatus;
import com.sni.bokaticowork.core.audit.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface AuditService {
    void save(AuditLog auditLog);

    Page<AuditLog> list(String module,
                        String action,
                        String actorEmail,
                        AuditStatus status,
                        Instant createdFrom,
                        Instant createdTo,
                        Pageable pageable);
}
