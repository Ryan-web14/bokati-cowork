package com.sni.bokaticowork.features.marketplace.repository;

import com.sni.bokaticowork.features.marketplace.model.MarketplaceOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MarketplaceOfferRepository extends JpaRepository<MarketplaceOffer, Long> {

    Optional<MarketplaceOffer> findByShortId(Long shortId);

    @Query("""
            SELECT o FROM MarketplaceOffer o
            WHERE (:published IS NULL OR o.published = :published)
              AND (:category IS NULL OR lower(o.category) = lower(CAST(:category AS string)))
            ORDER BY o.displayOrder ASC, o.title ASC
            """)
    List<MarketplaceOffer> search(@Param("published") Boolean published, @Param("category") String category);
}
