package com.sni.bokaticowork.features.subscription.rollover.repository;

import com.sni.bokaticowork.features.subscription.rollover.model.SubscriptionRolloverRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRolloverRecordRepository extends JpaRepository<SubscriptionRolloverRecord, Long> {

    @Query(nativeQuery = true, value = "SELECT * FROM subscription_rollover_record WHERE rollover_number = :rolloverNumber")
    Optional<SubscriptionRolloverRecord> findByRolloverNumber(@Param("rolloverNumber") String rolloverNumber);

    @Query(nativeQuery = true, value = """
            SELECT rr.*
            FROM subscription_rollover_record rr
            JOIN subscription s ON s.id = rr.subscription_id
            WHERE (:subscriptionNumber IS NULL OR s.subscription_number = :subscriptionNumber)
              AND (:entitlementCode IS NULL OR lower(rr.entitlement_code) = lower(:entitlementCode))
            ORDER BY rr.created_at DESC
            """)
    List<SubscriptionRolloverRecord> search(@Param("subscriptionNumber") String subscriptionNumber,
                                            @Param("entitlementCode") String entitlementCode);
}
