package com.sni.bokaticowork.features.support.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.support.dto.SupportDtos.*;
import com.sni.bokaticowork.features.support.enums.TicketStatus;
import com.sni.bokaticowork.features.support.service.interfaces.SupportTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/support/tickets")
public class SupportTicketController {
    private final SupportTicketService service;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPPORT:WRITE','SUPPORT_WRITE')")
    public ResponseEntity<SupportTicketResponse> create(@Valid @RequestBody CreateTicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPPORT:READ','SUPPORT_READ')")
    public ResponseEntity<PaginatedResponse<SupportTicketResponse>> search(@RequestParam(required = false) TicketStatus status,
                                                                           @RequestParam(required = false) Long assignedTo,
                                                                           @RequestParam(required = false) String ownerType,
                                                                           @RequestParam(required = false) String ownerCode,
                                                                           @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.search(status, assignedTo, ownerType, ownerCode, pageable));
    }

    @GetMapping("/{ticketNumber}")
    @PreAuthorize("hasAnyAuthority('SUPPORT:READ','SUPPORT_READ')")
    public ResponseEntity<SupportTicketResponse> get(@PathVariable String ticketNumber) {
        return ResponseEntity.ok(service.get(ticketNumber));
    }

    @PatchMapping("/{ticketNumber}/assign")
    @PreAuthorize("hasAnyAuthority('SUPPORT:ASSIGN','SUPPORT_ASSIGN')")
    public ResponseEntity<SupportTicketResponse> assign(@PathVariable String ticketNumber,
                                                        @RequestBody AssignTicketRequest request) {
        return ResponseEntity.ok(service.assign(ticketNumber, request));
    }

    @PatchMapping("/{ticketNumber}/status")
    @PreAuthorize("hasAnyAuthority('SUPPORT:WRITE','SUPPORT_WRITE')")
    public ResponseEntity<SupportTicketResponse> status(@PathVariable String ticketNumber,
                                                        @RequestBody UpdateTicketStatusRequest request) {
        return ResponseEntity.ok(service.updateStatus(ticketNumber, request));
    }

    @PostMapping("/{ticketNumber}/messages")
    @PreAuthorize("hasAnyAuthority('SUPPORT:WRITE','SUPPORT_WRITE')")
    public ResponseEntity<SupportTicketResponse> message(@PathVariable String ticketNumber,
                                                         @Valid @RequestBody AddTicketMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addMessage(ticketNumber, request));
    }

    @GetMapping("/metrics")
    @PreAuthorize("hasAnyAuthority('SUPPORT:METRICS','SUPPORT_METRICS')")
    public ResponseEntity<SupportMetricsResponse> metrics() {
        return ResponseEntity.ok(service.metrics());
    }
}
