package com.sni.bokaticowork.features.support.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.support.dto.KnowledgeDtos.ArticleSuggestionResponse;
import com.sni.bokaticowork.features.support.dto.KnowledgeDtos.KnowledgeArticleResponse;
import com.sni.bokaticowork.features.support.enums.TicketCategory;
import com.sni.bokaticowork.features.support.service.implementation.KnowledgeArticleServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/client/support/knowledge-base")
public class ClientKnowledgeArticleController {

    private final KnowledgeArticleServiceImpl service;

    @GetMapping
    public ResponseEntity<PaginatedResponse<KnowledgeArticleResponse>> search(
            @RequestParam(required = false) TicketCategory category,
            @RequestParam(required = false) String searchText,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.searchPublic(category, searchText, pageable));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<KnowledgeArticleResponse> read(@PathVariable String slug) {
        return ResponseEntity.ok(service.readPublic(slug));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<ArticleSuggestionResponse>> suggestions(
            @RequestParam(required = false) TicketCategory category,
            @RequestParam(required = false) String searchText,
            @RequestParam(required = false, defaultValue = "5") int limit) {
        return ResponseEntity.ok(service.suggest(true, category, searchText, limit));
    }
}
