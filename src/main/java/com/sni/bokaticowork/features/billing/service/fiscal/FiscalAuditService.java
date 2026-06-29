package com.sni.bokaticowork.features.billing.service.fiscal;

import com.sni.bokaticowork.features.billing.model.FiscalAuditLog;
import com.sni.bokaticowork.features.billing.repository.FiscalAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class FiscalAuditService {

    // Actions fiscales — utiliser ces constantes dans tout le codebase
    public static final String INVOICE_VALIDATED    = "INVOICE_VALIDATED";
    public static final String CREDIT_NOTE_CREATED  = "CREDIT_NOTE_CREATED";
    public static final String DEBIT_NOTE_CREATED   = "DEBIT_NOTE_CREATED";
    public static final String DOCUMENT_DELETED     = "DOCUMENT_DELETED";
    public static final String TAMPER_ATTEMPT       = "TAMPER_ATTEMPT";
    public static final String PDF_EXPORTED         = "PDF_EXPORTED";
    public static final String QUOTE_CONVERTED      = "QUOTE_CONVERTED";

    private final FiscalAuditLogRepository repository;

    public void log(String action, String entityType, String entityId) {
        log(action, entityType, entityId, null, null, null, null);
    }

    public void log(String action, String entityType, String entityId,
                    String oldValue, String newValue) {
        log(action, entityType, entityId, oldValue, newValue, null, null);
    }

    public void log(String action, String entityType, String entityId,
                    String oldValue, String newValue,
                    String ipAddress, String userAgent) {
        FiscalAuditLog entry = FiscalAuditLog.builder()
                .actorCode(resolveActor())
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .oldValue(oldValue)
                .newValue(newValue)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .createdAt(Instant.now())
                .build();
        repository.save(entry);
    }

    private String resolveActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "SYSTEM";
    }
}
