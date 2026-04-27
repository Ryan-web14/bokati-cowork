package com.sni.bokaticowork.features.subscription.promotion.repository;

import com.sni.bokaticowork.features.subscription.promotion.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
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
