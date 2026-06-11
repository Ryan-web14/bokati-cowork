package com.sni.bokaticowork.features.support.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.support.dto.KnowledgeDtos.*;
import com.sni.bokaticowork.features.support.enums.TicketCategory;
import com.sni.bokaticowork.features.support.service.implementation.KnowledgeArticleServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/support/knowledge-base")
public class KnowledgeArticleController {

    private final KnowledgeArticleServiceImpl service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPPORT:READ','SUPPORT_READ')")
    public ResponseEntity<PaginatedResponse<KnowledgeArticleResponse>> search(
            @RequestParam(required = false) TicketCategory category,
            @RequestParam(required = false) String searchText,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.search(category, searchText, pageable));
    }

    @GetMapping("/{articleCode}")
    @PreAuthorize("hasAnyAuthority('SUPPORT:READ','SUPPORT_READ')")
    public ResponseEntity<KnowledgeArticleResponse> get(@PathVariable String articleCode) {
        return ResponseEntity.ok(service.get(articleCode));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPPORT:WRITE','SUPPORT_WRITE')")
    public ResponseEntity<KnowledgeArticleResponse> create(@Valid @RequestBody KnowledgeArticleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{articleCode}")
    @PreAuthorize("hasAnyAuthority('SUPPORT:WRITE','SUPPORT_WRITE')")
    public ResponseEntity<KnowledgeArticleResponse> update(@PathVariable String articleCode,
                                                           @Valid @RequestBody KnowledgeArticleRequest request) {
        return ResponseEntity.ok(service.update(articleCode, request));
    }

    @DeleteMapping("/{articleCode}")
    @PreAuthorize("hasAnyAuthority('SUPPORT:WRITE','SUPPORT_WRITE')")
    public ResponseEntity<Void> delete(@PathVariable String articleCode) {
        service.delete(articleCode);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/suggestions")
    @PreAuthorize("hasAnyAuthority('SUPPORT:READ','SUPPORT_READ')")
    public ResponseEntity<List<ArticleSuggestionResponse>> suggestions(
            @RequestParam(required = false) TicketCategory category,
            @RequestParam(required = false) String searchText,
            @RequestParam(required = false, defaultValue = "5") int limit) {
        return ResponseEntity.ok(service.suggest(false, category, searchText, limit));
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyAuthority('SUPPORT:METRICS','SUPPORT_METRICS')")
    public ResponseEntity<KnowledgeAnalyticsResponse> analytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(service.analytics(from, to));
    }
}
