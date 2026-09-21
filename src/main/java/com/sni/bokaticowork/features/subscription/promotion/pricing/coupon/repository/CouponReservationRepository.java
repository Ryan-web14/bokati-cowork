package com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.repository;

import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.model.CouponReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CouponReservationRepository extends JpaRepository<CouponReservation, Long> {

    @Query("SELECT r FROM CouponReservation r WHERE r.reservationNumber = :reservationNumber")
    Optional<CouponReservation> findByReservationNumber(@Param("reservationNumber") String reservationNumber);

    @Query("""
            SELECT r FROM CouponReservation r
            WHERE r.cartReference = :cartReference
              AND r.status = com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.enums.CouponReservationStatus.RESERVED
            """)
    List<CouponReservation> findActiveByCart(@Param("cartReference") String cartReference);

    @Query("""
            SELECT r FROM CouponReservation r
            WHERE r.coupon.id = :couponId
              AND r.cartReference = :cartReference
              AND r.status = com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.enums.CouponReservationStatus.RESERVED
            """)
    Optional<CouponReservation> findActiveFor(@Param("couponId") Long couponId,
                                              @Param("cartReference") String cartReference);

    /** La retenue active d'un abonne sur un code, quel que soit le panier · celle qu'on capture a la souscription. */
    @Query("""
            SELECT r FROM CouponReservation r
            WHERE r.coupon.id = :couponId
              AND r.subscriberType = :subscriberType
              AND r.subscriberCode = :subscriberCode
              AND r.status = com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.enums.CouponReservationStatus.RESERVED
            ORDER BY r.reservedAt DESC
            """)
    List<CouponReservation> findActiveForSubscriber(@Param("couponId") Long couponId,
                                                    @Param("subscriberType") String subscriberType,
                                                    @Param("subscriberCode") String subscriberCode);

    /** Retenues arrivees a echeance · un panier abandonne ne doit pas immobiliser un code. */
    @Query("""
            SELECT r FROM CouponReservation r
            WHERE r.status = com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.enums.CouponReservationStatus.RESERVED
              AND r.expiresAt <= :now
            ORDER BY r.expiresAt
            """)
    List<CouponReservation> findExpired(@Param("now") Instant now);
}
