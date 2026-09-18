package com.sni.bokaticowork.features.subscription.promotion.pricing.audience.repository;

import com.sni.bokaticowork.features.subscription.promotion.pricing.audience.model.PromotionBeneficiary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PromotionBeneficiaryRepository extends JpaRepository<PromotionBeneficiary, Long> {

    @Query("""
            SELECT b FROM PromotionBeneficiary b
            WHERE b.promotion.id = :promotionId
              AND lower(b.subscriberType) = lower(:subscriberType)
              AND lower(b.subscriberCode) = lower(:subscriberCode)
            """)
    Optional<PromotionBeneficiary> find(@Param("promotionId") Long promotionId,
                                        @Param("subscriberType") String subscriberType,
                                        @Param("subscriberCode") String subscriberCode);

    /**
     * Promotions, parmi celles proposees, dont ce souscripteur est beneficiaire non retire.
     *
     * <p>Une seule requete pour toute une liste de campagnes : interroger promotion par promotion
     * ferait autant d'aller-retours que de campagnes actives, a chaque affichage de panier.</p>
     */
    @Query("""
            SELECT b.promotion.id FROM PromotionBeneficiary b
            WHERE b.promotion.id IN :promotionIds
              AND lower(b.subscriberType) = lower(:subscriberType)
              AND lower(b.subscriberCode) = lower(:subscriberCode)
              AND b.revokedAt IS NULL
            """)
    List<Long> findGrantedPromotionIds(@Param("promotionIds") Collection<Long> promotionIds,
                                       @Param("subscriberType") String subscriberType,
                                       @Param("subscriberCode") String subscriberCode);

    @Query("SELECT b FROM PromotionBeneficiary b WHERE b.promotion.id = :promotionId")
    Page<PromotionBeneficiary> findAllByPromotionId(@Param("promotionId") Long promotionId, Pageable pageable);

    @Query("""
            SELECT COUNT(b) FROM PromotionBeneficiary b
            WHERE b.promotion.id = :promotionId AND b.revokedAt IS NULL
            """)
    long countActive(@Param("promotionId") Long promotionId);

    @Query("""
            SELECT COUNT(b) FROM PromotionBeneficiary b
            WHERE b.promotion.id = :promotionId AND b.redeemedAt IS NOT NULL
            """)
    long countRedeemed(@Param("promotionId") Long promotionId);
}
