package com.sni.bokaticowork.features.subscription.promotion.pricing.audience.repository;

import com.sni.bokaticowork.features.subscription.promotion.pricing.audience.model.PromotionAudience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PromotionAudienceRepository extends JpaRepository<PromotionAudience, Long> {

    @Query("SELECT a FROM PromotionAudience a WHERE a.promotion.id IN :promotionIds")
    List<PromotionAudience> findAllByPromotionIds(@Param("promotionIds") Collection<Long> promotionIds);

    @Query("SELECT a FROM PromotionAudience a WHERE a.promotion.id = :promotionId")
    List<PromotionAudience> findAllByPromotionId(@Param("promotionId") Long promotionId);
}
