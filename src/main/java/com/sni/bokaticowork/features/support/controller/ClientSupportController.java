package com.sni.bokaticowork.features.support.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.support.dto.SupportDtos.*;
import com.sni.bokaticowork.features.support.enums.TicketSenderType;
import com.sni.bokaticowork.features.support.service.interfaces.SupportTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/client/support/tickets")
public class ClientSupportController {
    private final SupportTicketService service;

    @PostMapping
    public ResponseEntity<SupportTicketResponse> create(@Valid @RequestBody CreateTicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<SupportTicketResponse>> mine(@RequestParam String ownerType,
                                                                         @RequestParam String ownerCode,
                                                                         @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.search(null, null, ownerType, ownerCode, pageable));
    }

    @PostMapping("/{ticketNumber}/messages")
    public ResponseEntity<SupportTicketResponse> message(@PathVariable String ticketNumber,
                                                         @Valid @RequestBody AddTicketMessageRequest request) {
        AddTicketMessageRequest clientMessage = new AddTicketMessageRequest(
                TicketSenderType.CLIENT,
                request.senderId(),
                request.senderName(),
                request.message(),
                false
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addMessage(ticketNumber, clientMessage));
    }

    @PatchMapping("/{ticketNumber}/close")
    public ResponseEntity<SupportTicketResponse> close(@PathVariable String ticketNumber) {
        return ResponseEntity.ok(service.close(ticketNumber));
    }
}
