package com.sni.bokaticowork.features.company.sharing.repository;

import com.sni.bokaticowork.features.subscription.seat.model.SubscriptionSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

/**
 * Les places vues du partage d'abonnement · une place active est au statut ACTIVE.
 */
@Repository
public interface CompanySeatRepository extends JpaRepository<SubscriptionSeat, Long> {

    @Query("""
            SELECT s FROM SubscriptionSeat s
            WHERE s.subscription.id = :subscriptionId
              AND s.status = com.sni.bokaticowork.features.subscription.seat.enums.SeatStatus.ACTIVE
            ORDER BY s.createdAt ASC
            """)
    List<SubscriptionSeat> findActiveBySubscription(@Param("subscriptionId") Long subscriptionId);

    /** Les places actives d'un membre sur les abonnements d'une organisation. */
    @Query("""
            SELECT s FROM SubscriptionSeat s
            WHERE s.memberCode = :memberCode
              AND s.status = com.sni.bokaticowork.features.subscription.seat.enums.SeatStatus.ACTIVE
              AND s.subscription.subscriberType = com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType.CUSTOMER
            """)
    List<SubscriptionSeat> findActiveCompanySeatsOfMember(@Param("memberCode") String memberCode);

    @Query("""
            SELECT s FROM SubscriptionSeat s
            WHERE s.memberCode = :memberCode
              AND s.status = com.sni.bokaticowork.features.subscription.seat.enums.SeatStatus.ACTIVE
              AND s.subscription.subscriberType = com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType.CUSTOMER
              AND s.subscription.subscriberCode = :companyCode
            """)
    List<SubscriptionSeat> findActiveSeatsOfMemberInCompany(@Param("memberCode") String memberCode,
                                                            @Param("companyCode") String companyCode);

    Optional<SubscriptionSeat> findByShortId(Long shortId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SubscriptionSeat s WHERE s.id = :id")
    Optional<SubscriptionSeat> lockById(@Param("id") Long id);

    @Query("""
            SELECT COUNT(s) FROM SubscriptionSeat s
            WHERE s.subscription.id = :subscriptionId
              AND s.status = com.sni.bokaticowork.features.subscription.seat.enums.SeatStatus.ACTIVE
            """)
    long countActive(@Param("subscriptionId") Long subscriptionId);

    /** Verrou sur l'abonnement · les places puisent dans le meme pot, une a la fois. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT x FROM Subscription x WHERE x.id = :id")
    Optional<com.sni.bokaticowork.features.subscription.subscription.model.Subscription> lockSubscription(@Param("id") Long id);

    /** Les abonnements ouverts d'une organisation. */
    @Query("""
            SELECT x FROM Subscription x
            WHERE x.subscriberType = com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType.CUSTOMER
              AND x.subscriberCode = :companyCode
              AND x.status NOT IN (com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus.CANCELLED,
                                   com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus.EXPIRED,
                                   com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus.DRAFT)
            ORDER BY x.createdAt DESC
            """)
    List<com.sni.bokaticowork.features.subscription.subscription.model.Subscription> findOpenCompanySubscriptions(
            @Param("companyCode") String companyCode);
}
