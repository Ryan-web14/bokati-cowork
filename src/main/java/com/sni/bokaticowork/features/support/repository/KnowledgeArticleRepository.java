package com.sni.bokaticowork.features.support.repository;

import com.sni.bokaticowork.features.support.model.KnowledgeArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, Long> {

    Optional<KnowledgeArticle> findByArticleCode(String articleCode);

    Optional<KnowledgeArticle> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query(nativeQuery = true, value = """
            SELECT a.*
            FROM support_knowledge_article a
            WHERE a.active = TRUE
              AND (CAST(:publicOnly AS BOOLEAN) IS NOT TRUE OR a.public_visible = TRUE)
              AND (CAST(:category AS TEXT) IS NULL OR a.category = CAST(:category AS TEXT))
              AND (
                CAST(:searchText AS TEXT) IS NULL
                OR LOWER(a.title) LIKE LOWER('%' || CAST(:searchText AS TEXT) || '%')
                OR LOWER(a.body) LIKE LOWER('%' || CAST(:searchText AS TEXT) || '%')
                OR LOWER(COALESCE(a.tags, '')) LIKE LOWER('%' || CAST(:searchText AS TEXT) || '%')
              )
            ORDER BY a.view_count DESC, a.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM support_knowledge_article a
            WHERE a.active = TRUE
              AND (CAST(:publicOnly AS BOOLEAN) IS NOT TRUE OR a.public_visible = TRUE)
              AND (CAST(:category AS TEXT) IS NULL OR a.category = CAST(:category AS TEXT))
              AND (
                CAST(:searchText AS TEXT) IS NULL
                OR LOWER(a.title) LIKE LOWER('%' || CAST(:searchText AS TEXT) || '%')
                OR LOWER(a.body) LIKE LOWER('%' || CAST(:searchText AS TEXT) || '%')
                OR LOWER(COALESCE(a.tags, '')) LIKE LOWER('%' || CAST(:searchText AS TEXT) || '%')
              )
            """)
    Page<KnowledgeArticle> search(@Param("publicOnly") Boolean publicOnly,
                                  @Param("category") String category,
                                  @Param("searchText") String searchText,
                                  Pageable pageable);

    long countByActiveTrue();

    @Query("SELECT COALESCE(SUM(a.viewCount), 0) FROM KnowledgeArticle a")
    long sumViewCount();

    List<KnowledgeArticle> findTop5ByActiveTrueOrderByViewCountDesc();
}
