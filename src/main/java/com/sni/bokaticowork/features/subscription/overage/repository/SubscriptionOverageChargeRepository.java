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

    /** Les depassements des collaborateurs sur un abonnement, entre deux instants · pour l'annexe de facture. */
    @Query("""
            SELECT c FROM SubscriptionOverageCharge c JOIN FETCH c.usageRecord u
            WHERE c.subscription.id = :subscriptionId
              AND u.beneficiaryCode IS NOT NULL
              AND u.occurredAt >= :from AND u.occurredAt < :to
            """)
    List<SubscriptionOverageCharge> findBeneficiaryCharges(@Param("subscriptionId") Long subscriptionId,
                                                           @Param("from") java.time.Instant from,
                                                           @Param("to") java.time.Instant to);
}
