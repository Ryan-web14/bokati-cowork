package com.sni.bokaticowork.features.support.mapper;

import com.sni.bokaticowork.features.support.dto.KnowledgeDtos.ArticleViewEntry;
import com.sni.bokaticowork.features.support.dto.KnowledgeDtos.KnowledgeArticleResponse;
import com.sni.bokaticowork.features.support.model.KnowledgeArticle;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

@Component
public class KnowledgeArticleMapper {

    public KnowledgeArticleResponse toResponse(KnowledgeArticle article) {
        return new KnowledgeArticleResponse(
                article.getId(),
                article.getArticleCode(),
                article.getTitle(),
                article.getSlug(),
                article.getBody(),
                article.getCategory(),
                toTagList(article.getTags()),
                article.getPublicVisible(),
                article.getInternalOnly(),
                article.getActive(),
                article.getViewCount(),
                article.getCreatedBy(),
                article.getUpdatedBy(),
                article.getCreatedAt(),
                article.getUpdatedAt()
        );
    }

    public ArticleViewEntry toViewEntry(KnowledgeArticle article) {
        return new ArticleViewEntry(article.getArticleCode(), article.getTitle(), article.getViewCount());
    }

    public List<String> toTagList(String tags) {
        if (!StringUtils.hasText(tags)) return List.of();
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    public String toTagString(List<String> tags) {
        if (tags == null || tags.isEmpty()) return null;
        return tags.stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(String::toLowerCase)
                .distinct()
                .reduce((a, b) -> a + "," + b)
                .orElse(null);
    }
}
