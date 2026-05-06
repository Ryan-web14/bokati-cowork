package com.sni.bokaticowork.core.outbox.controller;

import com.sni.bokaticowork.core.outbox.enums.OutboxEventStatus;
import com.sni.bokaticowork.core.outbox.model.OutboxEvent;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/admin/outbox-events")
public class AdminOutboxController {

    private final OutboxService outboxService;

    @GetMapping
    public ResponseEntity<PaginatedResponse<OutboxEvent>> list(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String aggregateType,
            @RequestParam(required = false) String aggregateId,
            @RequestParam(required = false) OutboxEventStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(
                new PaginatedResponse<>(outboxService.list(
                        eventType,
                        aggregateType,
                        aggregateId,
                        status,
                        createdFrom,
                        createdTo,
                        pageable
                ))
        );
    }

    @PostMapping("/process")
    public ResponseEntity<Map<String, Integer>> process(@RequestParam(defaultValue = "25") int batchSize) {
        return ResponseEntity.ok(Map.of("processed", outboxService.processPending(batchSize)));
    }

    @PostMapping("/{id}/requeue")
    public ResponseEntity<Void> requeue(@PathVariable Long id) {
        outboxService.requeue(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/requeue-failed")
    public ResponseEntity<Map<String, Integer>> requeueFailed(
            @RequestParam(required = false) String aggregateType) {
        return ResponseEntity.ok(Map.of("requeued", outboxService.requeueFailed(aggregateType)));
    }
}
