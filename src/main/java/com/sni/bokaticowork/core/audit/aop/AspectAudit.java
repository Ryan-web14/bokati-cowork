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
                .ipAddress(request == null ? null : request.getRemoteAddr())
                .userAgent(request == null ? null : request.getHeader("User-Agent"))
                .sessionId(request == null ? null : (String) request.getAttribute("sessionId"))
                .metadata(metadata)
                .build();

        try{
            Object result = pjp.proceed();
            log.setDiff(AuditContext.getDiff());
            auditService.save(log);

            return result;
        }catch (Exception e){
            log.setAuditStatus(AuditStatus.FAILURE);
            log.setErrorCode(e.getClass().getSimpleName());
            log.setErrorMessage(safeMsg(e.getMessage()));
            metadata.put("exception", e.getClass().getSimpleName());
            log.setMetadata(metadata);

            auditService.save(log);
            throw e;
        }finally {
            AuditContext.clear();
        }
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
