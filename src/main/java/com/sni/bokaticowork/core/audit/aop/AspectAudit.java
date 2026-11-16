package com.sni.bokaticowork.core.audit.aop;


import com.sni.bokaticowork.core.audit.context.AuditContext;
import com.sni.bokaticowork.core.audit.context.SecurityAuditContextProvider;
import com.sni.bokaticowork.core.audit.enums.AuditStatus;
import com.sni.bokaticowork.core.audit.model.AuditLog;
import com.sni.bokaticowork.core.audit.service.interfaces.AuditService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
public class AspectAudit {

    private final AuditService auditService;
    private final SecurityAuditContextProvider ctx;

    @Around("@annotation(audited)")
    public Object Around(ProceedingJoinPoint pjp, Audited audited) throws Throwable {
        HttpServletRequest request = currentRequest();
        Map<String, Object> metadata = new HashMap<>(AuditContext.getMeta());
        metadata.put("method", pjp.getSignature().toShortString());
        if (request != null) {
            metadata.put("requestUri", request.getRequestURI());
            metadata.put("httpMethod", request.getMethod());
            if (request.getQueryString() != null) {
                metadata.put("queryString", request.getQueryString());
            }
        }

        AuditLog log = AuditLog.builder()
                .createdAt(Instant.now())
                .action(audited.action())
                .ressource(blankToNull(audited.ressource()))
                .actorEmail(ctx.emailOrSystem())
                .actorId(ctx.userIdOrNull() == null ? 0L : ctx.userIdOrNull())
                .auditStatus(AuditStatus.SUCCESS)
                .module(audited.module())
                .ipAddress(request == null ? null : com.sni.bokaticowork.core.observability.RequestContext.clientIp(request))
                .requestId(com.sni.bokaticowork.core.observability.RequestContext.currentRequestId())
                .userAgent(request == null ? null : request.getHeader("User-Agent"))
                .sessionId(request == null ? null : (String) request.getAttribute("sessionId"))
                .metadata(metadata)
                .build();

        // Lot A1 : les ecritures en base faites pendant l'appel (avant / apres, element touche).
        com.sni.bokaticowork.core.audit.change.AuditChanges.begin();
        boolean changesOpen = true;
        try{
            Object result = pjp.proceed();
            changesOpen = false;
            attachChanges(log, com.sni.bokaticowork.core.audit.change.AuditChanges.end(), true);
            auditService.save(log);

            return result;
        }catch (Exception e){
            if (changesOpen) {
                changesOpen = false;
                // Echec : la transaction est annulee, les ecritures captees n'ont pas eu lieu.
                attachChanges(log, com.sni.bokaticowork.core.audit.change.AuditChanges.end(), false);
            }
            log.setAuditStatus(AuditStatus.FAILURE);
            log.setErrorCode(e.getClass().getSimpleName());
            log.setErrorMessage(safeMsg(e.getMessage()));
            metadata.put("exception", e.getClass().getSimpleName());
            log.setMetadata(metadata);

            auditService.save(log);
            throw e;
        }finally {
            if (changesOpen) {
                com.sni.bokaticowork.core.audit.change.AuditChanges.end();
            }
            AuditContext.clear();
        }
    }

    /**
     * Diff explicite de l'appelant s'il en a pose un, sinon les changements captes (action reussie
     * seulement) ; element principal dans tous les cas.
     */
    private void attachChanges(AuditLog log, com.sni.bokaticowork.core.audit.change.AuditChanges.Result changes, boolean succeeded) {
        Map<String, Object> explicit = AuditContext.getDiff();
        if (explicit != null) {
            log.setDiff(explicit);
        } else if (succeeded && !changes.isEmpty()) {
            log.setDiff(changes.toDiff());
        }
        com.sni.bokaticowork.core.audit.change.AuditChanges.Change primary = changes.primary();
        if (primary != null) {
            log.setEntityType(primary.entityType());
            log.setEntityCode(truncate(primary.entityCode(), 120));
        }
    }

    private String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private String safeMsg(String msg) {
        if (msg == null) return null;
        return msg.length() > 1000 ? msg.substring(0, 1000) : msg;
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }
}
