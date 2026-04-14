package com.sni.bokaticowork.core.idempotency.controller;

import com.sni.bokaticowork.core.idempotency.enums.IdempotencyStatus;
import com.sni.bokaticowork.core.idempotency.model.IdempotencyRecord;
import com.sni.bokaticowork.core.idempotency.service.interfaces.IdempotencyAdminService;
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
@RequestMapping(ApiPath.V1 + "/admin/idempotency-records")
public class AdminIdempotencyController {

    private final IdempotencyAdminService service;

    @GetMapping
    public ResponseEntity<PaginatedResponse<IdempotencyRecord>> list(
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String idempotencyKey,
            @RequestParam(required = false) IdempotencyStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(new PaginatedResponse<>(
                service.list(operation, idempotencyKey, status, createdFrom, createdTo, pageable)
        ));
    }
}
