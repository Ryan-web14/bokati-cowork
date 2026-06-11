package com.sni.bokaticowork.features.support.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.support.dto.SupportDtos.*;
import com.sni.bokaticowork.features.support.enums.TicketSenderType;
import com.sni.bokaticowork.features.support.mapper.SupportTicketMapper;
import com.sni.bokaticowork.features.support.service.implementation.SupportAttachmentService;
import com.sni.bokaticowork.features.support.service.interfaces.SupportTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/client/support/tickets")
public class ClientSupportController {

    private final SupportTicketService service;
    private final SupportAttachmentService attachmentService;
    private final SupportTicketMapper mapper;

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
        return ResponseEntity.ok(service.search(null, null, ownerType, ownerCode,
                null, null, null, null, null, searchText, pageable));
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

    @PostMapping(value = "/{ticketNumber}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentResponse> uploadAttachment(@PathVariable String ticketNumber,
                                                               @RequestPart("file") MultipartFile file) {
        var attachment = attachmentService.upload(ticketNumber, file, false, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toAttachmentResponse(attachment));
    }

    @GetMapping("/{ticketNumber}/attachments")
    public ResponseEntity<List<AttachmentResponse>> listAttachments(@PathVariable String ticketNumber) {
        return ResponseEntity.ok(attachmentService.list(ticketNumber, false).stream().map(mapper::toAttachmentResponse).toList());
    }

    @GetMapping("/{ticketNumber}/attachments/{id}/download")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable String ticketNumber, @PathVariable Long id) {
        var file = attachmentService.download(ticketNumber, id, false);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(file.contentType()));
        headers.setContentDisposition(ContentDisposition.attachment().filename(file.fileName()).build());
        headers.setContentLength(file.content().length);
        return ResponseEntity.ok().headers(headers).body(file.content());
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
