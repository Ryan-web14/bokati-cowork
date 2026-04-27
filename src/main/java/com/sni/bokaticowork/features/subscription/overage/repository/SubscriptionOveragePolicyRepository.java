package com.sni.bokaticowork.features.subscription.overage.repository;

import com.sni.bokaticowork.features.subscription.overage.model.SubscriptionOveragePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionOveragePolicyRepository extends JpaRepository<SubscriptionOveragePolicy, Long> {

    @Query(nativeQuery = true, value = """
            SELECT sop.*
            FROM subscription_overage_policy sop
            JOIN entitlement_definition ed ON ed.id = sop.entitlement_definition_id
            WHERE sop.plan_version_id = :planVersionId
              AND lower(ed.code) = lower(:entitlementCode)
              AND sop.active = true
            ORDER BY sop.created_at DESC
            LIMIT 1
            """)
    Optional<SubscriptionOveragePolicy> findActivePolicy(@Param("planVersionId") Long planVersionId,
                                                         @Param("entitlementCode") String entitlementCode);

    @Query(nativeQuery = true, value = """
            SELECT sop.*
            FROM subscription_overage_policy sop
            JOIN entitlement_definition ed ON ed.id = sop.entitlement_definition_id
            WHERE (:planVersionId IS NULL OR sop.plan_version_id = :planVersionId)
              AND (:entitlementCode IS NULL OR lower(ed.code) = lower(:entitlementCode))
            ORDER BY sop.created_at DESC
            """)
    List<SubscriptionOveragePolicy> search(@Param("planVersionId") Long planVersionId,
                                           @Param("entitlementCode") String entitlementCode);
}
