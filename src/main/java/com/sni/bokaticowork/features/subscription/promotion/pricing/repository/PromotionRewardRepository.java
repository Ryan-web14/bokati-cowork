package com.sni.bokaticowork.features.subscription.promotion.pricing.repository;

import com.sni.bokaticowork.features.subscription.promotion.pricing.model.PromotionReward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PromotionRewardRepository extends JpaRepository<PromotionReward, Long> {

    @Query("SELECT r FROM PromotionReward r WHERE r.promotion.id IN :promotionIds")
    List<PromotionReward> findAllByPromotionIds(@Param("promotionIds") Collection<Long> promotionIds);

    @Query("SELECT r FROM PromotionReward r WHERE r.promotion.id = :promotionId")
    List<PromotionReward> findAllByPromotionId(@Param("promotionId") Long promotionId);
}
