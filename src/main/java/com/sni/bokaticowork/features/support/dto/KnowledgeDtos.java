package com.sni.bokaticowork.features.support.dto;

import com.sni.bokaticowork.features.support.enums.TicketCategory;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;

public class KnowledgeDtos {

    // ── Requêtes ──────────────────────────────────────────────────

    public record KnowledgeArticleRequest(
            @NotBlank String title,
            String slug,
            @NotBlank String body,
            TicketCategory category,
            List<String> tags,
            Boolean publicVisible,
            Boolean internalOnly,
            Boolean active
    ) {}

    public record ConvertMessageToArticleRequest(
            @NotBlank String title,
            String slug,
            TicketCategory category,
            List<String> tags,
            Boolean publicVisible
    ) {}

    // ── Réponses ─────────────────────────────────────────────────

    public record KnowledgeArticleResponse(
            Long id,
            String articleCode,
            String title,
            String slug,
            String body,
            TicketCategory category,
            List<String> tags,
            Boolean publicVisible,
            Boolean internalOnly,
            Boolean active,
            Long viewCount,
            Long createdBy,
            Long updatedBy,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record ArticleSuggestionResponse(
            String articleCode,
            String title,
            String slug,
            String excerpt
    ) {}

    public record ArticleViewEntry(
            String articleCode,
            String title,
            Long viewCount
    ) {}

    public record KnowledgeAnalyticsResponse(
            Instant from,
            Instant to,
            long totalArticles,
            long activeArticles,
            long totalViews,
            long ticketsCreatedInPeriod,
            List<ArticleViewEntry> topArticles
    ) {}
}
