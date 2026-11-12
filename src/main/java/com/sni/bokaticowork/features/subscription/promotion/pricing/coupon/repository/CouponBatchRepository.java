package com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.repository;

import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.model.CouponBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponBatchRepository extends JpaRepository<CouponBatch, Long> {

    /** Promotion lue avec le lot : la vue du lot affiche son code et son nom. */
    @EntityGraph(attributePaths = {"promotion"})
    @Query("SELECT b FROM CouponBatch b WHERE lower(b.batchCode) = lower(:batchCode)")
    Optional<CouponBatch> findByBatchCode(@Param("batchCode") String batchCode);

    /** Les lots, du plus recent au plus ancien, promotion chargee pour la vue. */
    @EntityGraph(attributePaths = {"promotion"})
    @Query(value = "SELECT b FROM CouponBatch b ORDER BY b.createdAt DESC, b.id DESC",
            countQuery = "SELECT count(b) FROM CouponBatch b")
    Page<CouponBatch> findRecent(Pageable pageable);

    /** Les lots d'une promotion, du plus recent au plus ancien. */
    @EntityGraph(attributePaths = {"promotion"})
    @Query(value = "SELECT b FROM CouponBatch b WHERE lower(b.promotion.code) = lower(:promotionCode) "
            + "ORDER BY b.createdAt DESC, b.id DESC",
            countQuery = "SELECT count(b) FROM CouponBatch b WHERE lower(b.promotion.code) = lower(:promotionCode)")
    Page<CouponBatch> findRecentByPromotionCode(@Param("promotionCode") String promotionCode, Pageable pageable);
}
