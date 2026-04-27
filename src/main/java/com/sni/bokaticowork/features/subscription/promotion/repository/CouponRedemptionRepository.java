package com.sni.bokaticowork.features.subscription.promotion.repository;

import com.sni.bokaticowork.features.subscription.promotion.model.CouponRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, Long> {

    @Query(nativeQuery = true, value = """
            SELECT EXISTS(
                SELECT 1
                FROM coupon_redemption cr
                JOIN promotion p ON p.id = cr.promotion_id
                WHERE lower(p.code) = lower(:promotionCode)
                  AND cr.subscriber_type = :subscriberType
                  AND cr.subscriber_code = :subscriberCode
            )
            """)
    boolean existsRedemption(@Param("promotionCode") String promotionCode,
                             @Param("subscriberType") String subscriberType,
                             @Param("subscriberCode") String subscriberCode);

    @Query(nativeQuery = true, value = "SELECT * FROM coupon_redemption WHERE subscription_id = :subscriptionId ORDER BY redeemed_at DESC")
    List<CouponRedemption> findAllBySubscriptionId(@Param("subscriptionId") Long subscriptionId);
}
