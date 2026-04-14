package com.sni.bokaticowork.core.audit.controller;

import com.sni.bokaticowork.core.audit.model.SettingsAuditLogs;
import com.sni.bokaticowork.core.audit.service.interfaces.SettingsAuditLogsService;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/admin/settings-audit-logs")
public class AdminSettingsAuditController {

    private final SettingsAuditLogsService service;

    @GetMapping
    public ResponseEntity<PaginatedResponse<SettingsAuditLogs>> list(
            @RequestParam(required = false) String settingKey,
            @RequestParam(required = false) Long changedBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant changedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant changedTo,
            @PageableDefault(size = 20, sort = "changedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(new PaginatedResponse<>(
                service.list(settingKey, changedBy, changedFrom, changedTo, pageable)
        ));
    }
}
