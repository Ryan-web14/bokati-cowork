package com.sni.bokaticowork.features.support.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.richtext.RichTextSupport;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.support.dto.KnowledgeDtos.ArticleSuggestionResponse;
import com.sni.bokaticowork.features.support.dto.KnowledgeDtos.ConvertMessageToArticleRequest;
import com.sni.bokaticowork.features.support.dto.KnowledgeDtos.KnowledgeAnalyticsResponse;
import com.sni.bokaticowork.features.support.dto.KnowledgeDtos.KnowledgeArticleRequest;
import com.sni.bokaticowork.features.support.dto.KnowledgeDtos.KnowledgeArticleResponse;
import com.sni.bokaticowork.features.support.enums.TicketCategory;
import com.sni.bokaticowork.features.support.mapper.KnowledgeArticleMapper;
import com.sni.bokaticowork.features.support.model.KnowledgeArticle;
import com.sni.bokaticowork.features.support.model.SupportTicket;
import com.sni.bokaticowork.features.support.model.TicketMessage;
import com.sni.bokaticowork.features.support.repository.KnowledgeArticleRepository;
import com.sni.bokaticowork.features.support.repository.SupportTicketRepository;
import com.sni.bokaticowork.features.support.repository.TicketMessageRepository;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@Transactional
@RequiredArgsConstructor
public class KnowledgeArticleServiceImpl {

    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_DASH = Pattern.compile("(^-|-$)");
    private static final int EXCERPT_LENGTH = 220;

    private final KnowledgeArticleRepository repository;
    private final SupportTicketRepository ticketRepository;
    private final TicketMessageRepository messageRepository;
    private final KnowledgeArticleMapper mapper;
    private final RichTextSupport richTextSupport;

    public KnowledgeArticleResponse create(KnowledgeArticleRequest request) {
        validate(request);
        Long userId = currentUserId();
        KnowledgeArticle article = KnowledgeArticle.builder()
                .articleCode("KB-" + Instant.now().toEpochMilli())
                .title(request.title().trim())
                .slug(uniqueSlug(StringUtils.hasText(request.slug()) ? request.slug() : request.title(), null))
                .body(richTextSupport.normalize(request.body()))
                .category(request.category())
                .tags(mapper.toTagString(request.tags()))
                .publicVisible(Boolean.TRUE.equals(request.publicVisible()))
                .internalOnly(request.internalOnly() == null || request.internalOnly())
                .active(request.active() == null || request.active())
                .createdBy(userId)
                .updatedBy(userId)
                .build();
        return mapper.toResponse(repository.save(article));
    }

    public KnowledgeArticleResponse update(String articleCode, KnowledgeArticleRequest request) {
        validate(request);
        KnowledgeArticle article = getArticle(articleCode);
        article.setTitle(request.title().trim());
        if (StringUtils.hasText(request.slug()) && !request.slug().trim().equalsIgnoreCase(article.getSlug())) {
            article.setSlug(uniqueSlug(request.slug(), article.getId()));
        }
        article.setBody(richTextSupport.normalize(request.body()));
        article.setCategory(request.category());
        article.setTags(mapper.toTagString(request.tags()));
        if (request.publicVisible() != null) article.setPublicVisible(request.publicVisible());
        if (request.internalOnly() != null) article.setInternalOnly(request.internalOnly());
        if (request.active() != null) article.setActive(request.active());
        article.setUpdatedBy(currentUserId());
        return mapper.toResponse(repository.save(article));
    }

    public void delete(String articleCode) {
        KnowledgeArticle article = getArticle(articleCode);
        article.setActive(false);
        article.setUpdatedBy(currentUserId());
        repository.save(article);
    }

    @Transactional(readOnly = true)
    public KnowledgeArticleResponse get(String articleCode) {
        return mapper.toResponse(getArticle(articleCode));
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<KnowledgeArticleResponse> search(TicketCategory category, String searchText, Pageable pageable) {
        String categoryStr = category != null ? category.name() : null;
        String text = StringUtils.hasText(searchText) ? searchText.trim() : null;
        return new PaginatedResponse<>(repository.search(false, categoryStr, text, pageable).map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<KnowledgeArticleResponse> searchPublic(TicketCategory category, String searchText, Pageable pageable) {
        String categoryStr = category != null ? category.name() : null;
        String text = StringUtils.hasText(searchText) ? searchText.trim() : null;
        return new PaginatedResponse<>(repository.search(true, categoryStr, text, pageable).map(mapper::toResponse));
    }

    public KnowledgeArticleResponse readPublic(String slug) {
        KnowledgeArticle article = repository.findBySlug(slug)
                .filter(a -> Boolean.TRUE.equals(a.getActive()) && Boolean.TRUE.equals(a.getPublicVisible()))
                .orElseThrow(() -> new ResourceNotFoundException("Knowledge article not found: " + slug));
        article.setViewCount(article.getViewCount() == null ? 1L : article.getViewCount() + 1);
        repository.save(article);
        return mapper.toResponse(article);
    }

    @Transactional(readOnly = true)
    public List<ArticleSuggestionResponse> suggest(boolean publicOnly, TicketCategory category, String searchText, int limit) {
        String categoryStr = category != null ? category.name() : null;
        String text = StringUtils.hasText(searchText) ? searchText.trim() : null;
        if (categoryStr == null && text == null) return List.of();
        int safeLimit = Math.max(1, Math.min(limit, 10));
        return repository.search(publicOnly, categoryStr, text, Pageable.ofSize(safeLimit)).stream()
                .map(this::toSuggestion)
                .toList();
    }

    public KnowledgeArticleResponse convertMessageToArticle(String ticketNumber, Long messageId, ConvertMessageToArticleRequest request) {
        if (request == null || !StringUtils.hasText(request.title())) {
            throw new BadRequestException("Le titre de l'article est requis");
        }
        SupportTicket ticket = ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketNumber));
        TicketMessage message = messageRepository.findById(messageId)
                .filter(m -> m.getTicket().getId().equals(ticket.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Message not found: " + messageId));
        Long userId = currentUserId();
        KnowledgeArticle article = KnowledgeArticle.builder()
                .articleCode("KB-" + Instant.now().toEpochMilli())
                .title(request.title().trim())
                .slug(uniqueSlug(StringUtils.hasText(request.slug()) ? request.slug() : request.title(), null))
                .body(richTextSupport.normalize(message.getMessage()))
                .category(request.category() != null ? request.category() : ticket.getCategory())
                .tags(mapper.toTagString(request.tags()))
                .publicVisible(Boolean.TRUE.equals(request.publicVisible()))
                .internalOnly(!Boolean.TRUE.equals(request.publicVisible()))
                .active(true)
                .createdBy(userId)
                .updatedBy(userId)
                .build();
        return mapper.toResponse(repository.save(article));
    }

    @Transactional(readOnly = true)
    public KnowledgeAnalyticsResponse analytics(Instant from, Instant to) {
        Instant start = from != null ? from : Instant.now().minusSeconds(30L * 24 * 3600);
        Instant end = to != null ? to : Instant.now();
        var topArticles = repository.findTop5ByActiveTrueOrderByViewCountDesc().stream()
                .map(mapper::toViewEntry)
                .toList();
        return new KnowledgeAnalyticsResponse(
                start,
                end,
                repository.count(),
                repository.countByActiveTrue(),
                repository.sumViewCount(),
                ticketRepository.countCreatedBetween(start, end),
                topArticles
        );
    }

    private ArticleSuggestionResponse toSuggestion(KnowledgeArticle article) {
        String body = article.getBody() == null ? "" : article.getBody().replaceAll("<[^>]*>", " ").trim();
        String excerpt = body.length() > EXCERPT_LENGTH ? body.substring(0, EXCERPT_LENGTH).trim() + "…" : body;
        return new ArticleSuggestionResponse(article.getArticleCode(), article.getTitle(), article.getSlug(), excerpt);
    }

    private KnowledgeArticle getArticle(String articleCode) {
        return repository.findByArticleCode(articleCode)
                .orElseThrow(() -> new ResourceNotFoundException("Knowledge article not found: " + articleCode));
    }

    private String uniqueSlug(String base, Long currentId) {
        String slug = slugify(base);
        String candidate = slug;
        int suffix = 2;
        while (true) {
            var existing = repository.findBySlug(candidate);
            if (existing.isEmpty() || (currentId != null && existing.get().getId().equals(currentId))) {
                return candidate;
            }
            candidate = slug + "-" + suffix++;
        }
    }

    private String slugify(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        String slug = EDGE_DASH.matcher(NON_ALNUM.matcher(normalized).replaceAll("-")).replaceAll("");
        return StringUtils.hasText(slug) ? slug : "article-" + Instant.now().toEpochMilli();
    }

    private void validate(KnowledgeArticleRequest request) {
        if (request == null || !StringUtils.hasText(request.title())) {
            throw new BadRequestException("Le titre de l'article est requis");
        }
        if (!StringUtils.hasText(request.body())) {
            throw new BadRequestException("Le contenu de l'article est requis");
        }
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUser().getId();
        }
        return null;
    }
}
