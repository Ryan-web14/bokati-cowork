package com.sni.bokaticowork.features.subscription.promotion.repository;

import com.sni.bokaticowork.features.subscription.promotion.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long>, JpaSpecificationExecutor<Promotion> {

    @Query(nativeQuery = true, value = "SELECT EXISTS(SELECT 1 FROM promotion WHERE lower(code) = lower(:code))")
    boolean existsByCodeIgnoreCase(@Param("code") String code);

    @Query(nativeQuery = true, value = "SELECT * FROM promotion WHERE lower(code) = lower(:code)")
    Optional<Promotion> findByCodeIgnoreCase(@Param("code") String code);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM promotion
            WHERE lower(code) = lower(:code)
              AND status = 'ACTIVE'
              AND starts_at <= :now
              AND (ends_at IS NULL OR ends_at >= :now)
            """)
    Optional<Promotion> findUsablePromotion(@Param("code") String code, @Param("now") Instant now);

    /**
     * Promotions candidates a une evaluation automatique : actives, dans leur fenetre, non
     * epuisees en nombre d'utilisations et non epuisees en budget. Le tri par priorite est celui
     * de l'evaluation, il appartient donc a la requete et non a l'appelant.
     */
    @Query(nativeQuery = true, value = """
            SELECT *
            FROM promotion
            WHERE status = 'ACTIVE'
              AND promotion_type = CAST(:promotionType AS VARCHAR)
              AND starts_at <= :now
              AND (ends_at IS NULL OR ends_at >= :now)
              AND (max_redemptions IS NULL OR redemption_count < max_redemptions)
              AND (budget_amount IS NULL OR consumed_budget_amount < budget_amount)
            ORDER BY priority ASC, id ASC
            """)
    List<Promotion> findEvaluable(@Param("promotionType") String promotionType, @Param("now") Instant now);

    @Modifying
    @Query(nativeQuery = true, value = """
            UPDATE promotion
            SET status = 'EXPIRED',
                updated_at = now()
            WHERE status = 'ACTIVE'
              AND ends_at IS NOT NULL
              AND ends_at < :now
            """)
    int expireEndedPromotions(@Param("now") Instant now);
}
