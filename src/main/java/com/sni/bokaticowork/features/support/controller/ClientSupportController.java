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
    public ResponseEntity<PaginatedResponse<SupportTicketResponse>> mine(
            @RequestParam String ownerType,
            @RequestParam String ownerCode,
            @RequestParam(required = false) String searchText,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.search(null, null, ownerType, ownerCode, searchText, pageable));
    }

    @GetMapping("/{ticketNumber}")
    public ResponseEntity<SupportTicketResponse> get(@PathVariable String ticketNumber) {
        return ResponseEntity.ok(service.get(ticketNumber));
    }

    @PostMapping("/{ticketNumber}/messages")
    public ResponseEntity<SupportTicketResponse> message(@PathVariable String ticketNumber,
                                                         @Valid @RequestBody AddTicketMessageRequest request) {
        AddTicketMessageRequest clientMsg = new AddTicketMessageRequest(
                TicketSenderType.CLIENT, request.senderId(), request.senderName(), request.content(), false);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addMessage(ticketNumber, clientMsg));
    }

    @PatchMapping("/{ticketNumber}/close")
    public ResponseEntity<SupportTicketResponse> close(@PathVariable String ticketNumber) {
        return ResponseEntity.ok(service.close(ticketNumber));
    }

    @PostMapping("/{ticketNumber}/csat")
    public ResponseEntity<SupportTicketResponse> csat(@PathVariable String ticketNumber,
                                                      @Valid @RequestBody SubmitCsatRequest request) {
        return ResponseEntity.ok(service.submitCsat(ticketNumber, request));
    }
}
