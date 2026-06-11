package com.sni.bokaticowork.features.support.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.support.dto.KnowledgeDtos.ConvertMessageToArticleRequest;
import com.sni.bokaticowork.features.support.dto.KnowledgeDtos.KnowledgeArticleResponse;
import com.sni.bokaticowork.features.support.dto.SupportDtos.*;
import com.sni.bokaticowork.features.support.enums.TicketCategory;
import com.sni.bokaticowork.features.support.enums.TicketPriority;
import com.sni.bokaticowork.features.support.enums.TicketStatus;
import com.sni.bokaticowork.features.support.mapper.SupportTicketMapper;
import com.sni.bokaticowork.features.support.service.implementation.KnowledgeArticleServiceImpl;
import com.sni.bokaticowork.features.support.service.implementation.SupportAttachmentService;
import com.sni.bokaticowork.features.support.service.interfaces.SupportTicketService;
import com.sni.bokaticowork.features.task.dto.TaskDtos.TaskResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/support/tickets")
public class SupportTicketController {

    private final SupportTicketService service;
    private final SupportAttachmentService attachmentService;
    private final SupportTicketMapper mapper;
    private final KnowledgeArticleServiceImpl knowledgeArticleService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPPORT:WRITE','SUPPORT_WRITE')")
    public ResponseEntity<SupportTicketResponse> create(@Valid @RequestBody CreateTicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPPORT:READ','SUPPORT_READ')")
    public ResponseEntity<PaginatedResponse<SupportTicketResponse>> search(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) Long assignedTo,
            @RequestParam(required = false) String ownerType,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) TicketCategory category,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) String relatedType,
            @RequestParam(required = false) String relatedCode,
            @RequestParam(required = false) Boolean overdueOnly,
            @RequestParam(required = false) String searchText,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.search(status, assignedTo, ownerType, ownerCode,
                category, priority, relatedType, relatedCode, overdueOnly, searchText, pageable));
    }

    @GetMapping("/{ticketNumber}")
    @PreAuthorize("hasAnyAuthority('SUPPORT:READ','SUPPORT_READ')")
    public ResponseEntity<SupportTicketResponse> get(@PathVariable String ticketNumber) {
        return ResponseEntity.ok(service.get(ticketNumber));
    }

    @GetMapping("/{ticketNumber}/timeline")
    @PreAuthorize("hasAnyAuthority('SUPPORT:READ','SUPPORT_READ')")
    public ResponseEntity<List<TicketEventResponse>> timeline(@PathVariable String ticketNumber) {
        return ResponseEntity.ok(service.getTimeline(ticketNumber));
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

    @PostMapping("/{ticketNumber}/messages/{messageId}/convert-to-article")
    @PreAuthorize("hasAnyAuthority('SUPPORT:WRITE','SUPPORT_WRITE')")
    public ResponseEntity<KnowledgeArticleResponse> convertMessageToArticle(@PathVariable String ticketNumber,
                                                                            @PathVariable Long messageId,
                                                                            @Valid @RequestBody ConvertMessageToArticleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(knowledgeArticleService.convertMessageToArticle(ticketNumber, messageId, request));
    }

    @PostMapping(value = "/{ticketNumber}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('SUPPORT:WRITE','SUPPORT_WRITE')")
    public ResponseEntity<AttachmentResponse> uploadAttachment(@PathVariable String ticketNumber,
                                                               @RequestPart("file") MultipartFile file,
                                                               @RequestParam(required = false) Boolean internal,
                                                               @RequestParam(required = false) Long messageId) {
        var attachment = attachmentService.upload(ticketNumber, file, Boolean.TRUE.equals(internal), messageId);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toAttachmentResponse(attachment));
    }

    @GetMapping("/{ticketNumber}/attachments")
    @PreAuthorize("hasAnyAuthority('SUPPORT:READ','SUPPORT_READ')")
    public ResponseEntity<List<AttachmentResponse>> listAttachments(@PathVariable String ticketNumber) {
        return ResponseEntity.ok(attachmentService.list(ticketNumber, true).stream().map(mapper::toAttachmentResponse).toList());
    }

    @GetMapping("/{ticketNumber}/attachments/{id}/download")
    @PreAuthorize("hasAnyAuthority('SUPPORT:READ','SUPPORT_READ')")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable String ticketNumber, @PathVariable Long id) {
        var file = attachmentService.download(ticketNumber, id, true);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(file.contentType()));
        headers.setContentDisposition(ContentDisposition.attachment().filename(file.fileName()).build());
        headers.setContentLength(file.content().length);
        return ResponseEntity.ok().headers(headers).body(file.content());
    }

    @DeleteMapping("/{ticketNumber}/attachments/{id}")
    @PreAuthorize("hasAnyAuthority('SUPPORT:WRITE','SUPPORT_WRITE')")
    public ResponseEntity<Void> deleteAttachment(@PathVariable String ticketNumber, @PathVariable Long id) {
        attachmentService.delete(ticketNumber, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{ticketNumber}/tags")
    @PreAuthorize("hasAnyAuthority('SUPPORT:WRITE','SUPPORT_WRITE')")
    public ResponseEntity<SupportTicketResponse> addTag(@PathVariable String ticketNumber,
                                                         @Valid @RequestBody AddTagRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addTag(ticketNumber, request));
    }

    @DeleteMapping("/{ticketNumber}/tags/{tag}")
    @PreAuthorize("hasAnyAuthority('SUPPORT:WRITE','SUPPORT_WRITE')")
    public ResponseEntity<SupportTicketResponse> removeTag(@PathVariable String ticketNumber, @PathVariable String tag) {
        return ResponseEntity.ok(service.removeTag(ticketNumber, tag));
    }

    @PostMapping("/{ticketNumber}/task")
    @PreAuthorize("hasAnyAuthority('SUPPORT:WRITE','SUPPORT_WRITE') and hasAnyAuthority('TASK:WRITE','TASK_WRITE')")
    public ResponseEntity<TaskResponse> createTask(@PathVariable String ticketNumber,
                                                    @Valid @RequestBody CreateTicketTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createTask(ticketNumber, request));
    }

    @GetMapping("/by-owner")
    @PreAuthorize("hasAnyAuthority('SUPPORT:READ','SUPPORT_READ')")
    public ResponseEntity<OwnerTicketSummaryResponse> byOwner(@RequestParam String ownerType,
                                                              @RequestParam String ownerCode) {
        return ResponseEntity.ok(service.ownerSummary(ownerType, ownerCode));
    }

    @GetMapping("/metrics")
    @PreAuthorize("hasAnyAuthority('SUPPORT:METRICS','SUPPORT_METRICS')")
    public ResponseEntity<SupportMetricsResponse> metrics() {
        return ResponseEntity.ok(service.metrics());
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyAuthority('SUPPORT:METRICS','SUPPORT_METRICS')")
    public ResponseEntity<SupportAnalyticsResponse> analytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(service.analytics(from, to));
    }

    @GetMapping("/analytics.csv")
    @PreAuthorize("hasAnyAuthority('SUPPORT:METRICS','SUPPORT_METRICS')")
    public ResponseEntity<String> analyticsCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=support-analytics.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(service.analyticsCsv(from, to));
    }

    @GetMapping("/agents/performance")
    @PreAuthorize("hasAnyAuthority('SUPPORT:METRICS','SUPPORT_METRICS')")
    public ResponseEntity<List<AgentPerformanceEntry>> agentPerformance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(service.agentPerformance(from, to));
    }
}
