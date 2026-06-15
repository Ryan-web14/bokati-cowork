package com.sni.bokaticowork.features.support.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.portal.context.ClientContextService;
import com.sni.bokaticowork.features.portal.support.dto.request.ClientAddMessageRequest;
import com.sni.bokaticowork.features.portal.support.dto.request.ClientCreateTicketRequest;
import com.sni.bokaticowork.features.portal.support.service.ClientSupportService;
import com.sni.bokaticowork.features.support.dto.SupportDtos.AttachmentResponse;
import com.sni.bokaticowork.features.support.dto.SupportDtos.OwnerTicketSummaryResponse;
import com.sni.bokaticowork.features.support.dto.SupportDtos.SubmitCsatRequest;
import com.sni.bokaticowork.features.support.dto.SupportDtos.SupportTicketResponse;
import com.sni.bokaticowork.features.support.enums.TicketCategory;
import com.sni.bokaticowork.features.support.enums.TicketStatus;
import com.sni.bokaticowork.features.support.service.implementation.SupportAttachmentService.DownloadedAttachment;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/client/support/tickets")
public class ClientSupportController {

    private final ClientContextService clientContextService;
    private final ClientSupportService clientSupportService;

    @PostMapping
    public ResponseEntity<SupportTicketResponse> create(
            @Valid @RequestBody ClientCreateTicketRequest request) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clientSupportService.createTicket(member, request));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<SupportTicketResponse>> list(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketCategory category,
            @RequestParam(required = false) String searchText,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientSupportService.listMyTickets(member, status, category, searchText, pageable));
    }

    @GetMapping("/summary")
    public ResponseEntity<OwnerTicketSummaryResponse> summary() {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientSupportService.getSummary(member));
    }

    @GetMapping("/{ticketNumber}")
    public ResponseEntity<SupportTicketResponse> get(@PathVariable String ticketNumber) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientSupportService.getTicket(member, ticketNumber));
    }

    @PostMapping("/{ticketNumber}/messages")
    public ResponseEntity<SupportTicketResponse> addMessage(
            @PathVariable String ticketNumber,
            @Valid @RequestBody ClientAddMessageRequest request) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clientSupportService.addMessage(member, ticketNumber, request));
    }

    @PostMapping(value = "/{ticketNumber}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentResponse> uploadAttachment(
            @PathVariable String ticketNumber,
            @RequestPart("file") MultipartFile file) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clientSupportService.uploadAttachment(member, ticketNumber, file));
    }

    @GetMapping("/{ticketNumber}/attachments")
    public ResponseEntity<List<AttachmentResponse>> listAttachments(@PathVariable String ticketNumber) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientSupportService.listAttachments(member, ticketNumber));
    }

    @GetMapping("/{ticketNumber}/attachments/{id}/download")
    public ResponseEntity<byte[]> downloadAttachment(
            @PathVariable String ticketNumber,
            @PathVariable Long id) {
        Member member = clientContextService.getAuthenticatedMember();
        DownloadedAttachment file = clientSupportService.downloadAttachment(member, ticketNumber, id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(file.contentType()));
        headers.setContentDisposition(ContentDisposition.attachment().filename(file.fileName()).build());
        headers.setContentLength(file.content().length);
        return ResponseEntity.ok().headers(headers).body(file.content());
    }

    @PatchMapping("/{ticketNumber}/close")
    public ResponseEntity<SupportTicketResponse> close(@PathVariable String ticketNumber) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientSupportService.closeTicket(member, ticketNumber));
    }

    @PostMapping("/{ticketNumber}/csat")
    public ResponseEntity<SupportTicketResponse> csat(
            @PathVariable String ticketNumber,
            @Valid @RequestBody SubmitCsatRequest request) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientSupportService.submitCsat(member, ticketNumber, request));
    }
}
