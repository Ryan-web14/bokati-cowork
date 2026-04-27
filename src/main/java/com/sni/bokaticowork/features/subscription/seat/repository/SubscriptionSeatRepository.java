package com.sni.bokaticowork.features.subscription.seat.repository;

import com.sni.bokaticowork.features.subscription.seat.model.SubscriptionSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionSeatRepository extends JpaRepository<SubscriptionSeat, Long>, JpaSpecificationExecutor<SubscriptionSeat> {

    @Query(nativeQuery = true, value = "SELECT * FROM subscription_seat WHERE subscription_id = :subscriptionId ORDER BY created_at DESC")
    List<SubscriptionSeat> findAllBySubscriptionId(@Param("subscriptionId") Long subscriptionId);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM subscription_seat
            WHERE subscription_id = :subscriptionId
              AND member_code = :memberCode
              AND status <> 'REMOVED'
            LIMIT 1
            """)
    Optional<SubscriptionSeat> findActiveSeat(@Param("subscriptionId") Long subscriptionId, @Param("memberCode") String memberCode);
}
