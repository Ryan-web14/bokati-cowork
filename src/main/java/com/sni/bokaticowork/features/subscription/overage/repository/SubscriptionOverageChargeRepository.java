package com.sni.bokaticowork.features.subscription.overage.repository;

import com.sni.bokaticowork.features.subscription.overage.model.SubscriptionOverageCharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionOverageChargeRepository extends JpaRepository<SubscriptionOverageCharge, Long> {

    @Query(nativeQuery = true, value = "SELECT * FROM subscription_overage_charge WHERE charge_number = :chargeNumber")
    Optional<SubscriptionOverageCharge> findByChargeNumber(@Param("chargeNumber") String chargeNumber);

    @Query(nativeQuery = true, value = """
            SELECT soc.*
            FROM subscription_overage_charge soc
            LEFT JOIN subscription s ON s.id = soc.subscription_id
            WHERE (:subscriptionNumber IS NULL OR s.subscription_number = :subscriptionNumber)
              AND (:ownerType IS NULL OR soc.owner_type = :ownerType)
              AND (:ownerCode IS NULL OR soc.owner_code = :ownerCode)
              AND (:entitlementCode IS NULL OR lower(soc.entitlement_code) = lower(:entitlementCode))
            ORDER BY soc.created_at DESC
            """)
    List<SubscriptionOverageCharge> search(@Param("subscriptionNumber") String subscriptionNumber,
                                           @Param("ownerType") String ownerType,
                                           @Param("ownerCode") String ownerCode,
                                           @Param("entitlementCode") String entitlementCode);
}
