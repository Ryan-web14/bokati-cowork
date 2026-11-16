package com.sni.bokaticowork.core.audit.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.audit.dto.request.AuditLogFilter;
import com.sni.bokaticowork.core.audit.enums.AuditStatus;
import com.sni.bokaticowork.core.audit.model.AuditLog;
import com.sni.bokaticowork.core.audit.service.interfaces.AuditService;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Journal des actions (lot A1) : qui a fait quoi, quand, d'ou, sur quel element, avec l'avant et
 * l'apres. Droit {@code SYSTEM:AUDIT}.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/admin/audit-logs")
public class AdminAuditController {

    private final AuditService auditService;

    @Value("${bokati.audit.export.max-rows:10000}")
    private int exportMaxRows;

    @GetMapping
    public ResponseEntity<PaginatedResponse<AuditLog>> list(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actorEmail,
            @RequestParam(required = false) AuditStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityCode,
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) String ressource,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        AuditLogFilter filter = new AuditLogFilter(module, action, actorEmail, status, createdFrom, createdTo,
                entityType, entityCode, requestId, ressource);
        return ResponseEntity.ok(new PaginatedResponse<>(auditService.list(filter, pageable)));
    }

    /** Export CSV (point-virgule, UTF-8) avec les memes filtres ; l'export est lui-meme journalise. */
    @GetMapping("/export")
    @Audited(module = "AUDIT", action = "EXPORT_AUDIT_LOGS", ressource = "audit_log")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actorEmail,
            @RequestParam(required = false) AuditStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityCode,
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) String ressource) {
        AuditLogFilter filter = new AuditLogFilter(module, action, actorEmail, status, createdFrom, createdTo,
                entityType, entityCode, requestId, ressource);
        AuditService.AuditExport export = auditService.export(filter, exportMaxRows);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("journal-actions-" + LocalDate.now() + ".csv").build());
        headers.add("X-Export-Rows", String.valueOf(export.rows()));
        headers.add("X-Export-Truncated", String.valueOf(export.truncated()));
        headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "X-Export-Rows, X-Export-Truncated, Content-Disposition");
        return ResponseEntity.ok().headers(headers).body(export.csv().getBytes(StandardCharsets.UTF_8));
    }
}
