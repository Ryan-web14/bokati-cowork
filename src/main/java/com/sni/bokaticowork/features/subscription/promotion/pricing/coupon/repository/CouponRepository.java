package com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.repository;

import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.enums.CouponStatus;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.model.Coupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    /** Promotion et lot lus avec le coupon : CouponResponse.from les lit hors transaction. */
    @EntityGraph(attributePaths = {"promotion", "batch"})
    @Query("SELECT c FROM Coupon c WHERE lower(c.code) = lower(:code)")
    Optional<Coupon> findByCode(@Param("code") String code);

    /**
     * Lit le coupon sous verrou exclusif.
     *
     * <p>Sans ce verrou, deux paniers simultanes lisent tous deux « une utilisation restante » et
     * la consomment tous deux. C'est le cas courant d'un code partage sur les reseaux, pas une
     * hypothese de laboratoire : le verrou optimiste porte par l'entite ne ferait qu'en faire
     * echouer un apres coup, alors qu'on veut le faire attendre.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE lower(c.code) = lower(:code)")
    Optional<Coupon> findByCodeForUpdate(@Param("code") String code);

    @Query("SELECT c FROM Coupon c WHERE c.batch.id = :batchId")
    List<Coupon> findAllByBatchId(@Param("batchId") Long batchId);

    /** Tous les codes d'un lot dans l'ordre alphabetique, pour l'export remis au partenaire. */
    @Query("SELECT c FROM Coupon c WHERE c.batch.id = :batchId ORDER BY c.code")
    List<Coupon> findAllByBatchIdOrderByCode(@Param("batchId") Long batchId);

    /** Les codes d'un lot, page par page, dans l'ordre alphabetique pour qu'une page reste stable. */
    @Query(value = "SELECT c FROM Coupon c WHERE c.batch.id = :batchId ORDER BY c.code",
            countQuery = "SELECT count(c) FROM Coupon c WHERE c.batch.id = :batchId")
    Page<Coupon> findPageByBatchId(@Param("batchId") Long batchId, Pageable pageable);

    /** Les codes d'un lot dans un statut donne, par exemple ceux qui restent a distribuer. */
    @Query(value = "SELECT c FROM Coupon c WHERE c.batch.id = :batchId AND c.status = :status ORDER BY c.code",
            countQuery = "SELECT count(c) FROM Coupon c WHERE c.batch.id = :batchId AND c.status = :status")
    Page<Coupon> findPageByBatchIdAndStatus(@Param("batchId") Long batchId,
                                            @Param("status") CouponStatus status,
                                            Pageable pageable);

    /**
     * Compteurs vivants par lot et par statut, en une requete pour toute une page de lots. Le
     * compteur stocke sur le lot n'est pas tenu a jour par la consommation : seuls les coupons
     * disent ou en est la campagne.
     */
    @Query("""
            SELECT c.batch.id AS batchId,
                   c.status AS status,
                   count(c) AS couponCount,
                   coalesce(sum(c.redemptionCount), 0) AS redemptions,
                   coalesce(sum(c.reservedCount), 0) AS reserved
            FROM Coupon c
            WHERE c.batch.id IN :batchIds
            GROUP BY c.batch.id, c.status
            """)
    List<BatchStatusCount> countByStatusForBatches(@Param("batchIds") Collection<Long> batchIds);

    /**
     * Une ligne de comptage : un lot, un statut, et ce que ses coupons totalisent. Des nombres
     * plutot que des Long, la somme d'une colonne entiere n'ayant pas le meme type selon le dialecte.
     */
    interface BatchStatusCount {
        Long getBatchId();

        CouponStatus getStatus();

        Number getCouponCount();

        Number getRedemptions();

        Number getReserved();
    }

    /**
     * Coupons dont un abonne dispose et qu'il peut encore utiliser. Un coupon nominatif n'apparait
     * qu'a son titulaire, ce qui est la raison d'etre du ciblage.
     */
    @Query(nativeQuery = true, value = """
            SELECT c.*
            FROM coupon c
            JOIN promotion p ON p.id = c.promotion_id
            WHERE c.status = 'ACTIVE'
              AND p.status = 'ACTIVE'
              AND (c.valid_from IS NULL OR c.valid_from <= :now)
              AND (c.valid_until IS NULL OR c.valid_until >= :now)
              AND (c.max_redemptions IS NULL OR c.redemption_count < c.max_redemptions)
              AND (
                    c.assigned_to_code IS NULL
                    OR (c.assigned_to_type = CAST(:subscriberType AS VARCHAR)
                        AND c.assigned_to_code = CAST(:subscriberCode AS VARCHAR))
                  )
            ORDER BY c.valid_until NULLS LAST, c.id
            """)
    List<Coupon> findAvailableFor(@Param("subscriberType") String subscriberType,
                                  @Param("subscriberCode") String subscriberCode,
                                  @Param("now") java.time.Instant now);

    @Query(nativeQuery = true, value = "SELECT EXISTS(SELECT 1 FROM coupon WHERE lower(code) = lower(:code))")
    boolean existsByCode(@Param("code") String code);
}
