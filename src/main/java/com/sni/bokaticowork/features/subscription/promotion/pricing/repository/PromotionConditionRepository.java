package com.sni.bokaticowork.features.subscription.promotion.pricing.repository;

import com.sni.bokaticowork.features.subscription.promotion.pricing.model.PromotionCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PromotionConditionRepository extends JpaRepository<PromotionCondition, Long> {

    @Query("SELECT c FROM PromotionCondition c WHERE c.promotion.id IN :promotionIds")
    List<PromotionCondition> findAllByPromotionIds(@Param("promotionIds") Collection<Long> promotionIds);

    @Query("SELECT c FROM PromotionCondition c WHERE c.promotion.id = :promotionId")
    List<PromotionCondition> findAllByPromotionId(@Param("promotionId") Long promotionId);
}
