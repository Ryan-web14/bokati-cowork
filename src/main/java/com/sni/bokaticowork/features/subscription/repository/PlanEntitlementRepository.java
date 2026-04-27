package com.sni.bokaticowork.features.subscription.repository;

import com.sni.bokaticowork.features.subscription.subscription.model.PlanEntitlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanEntitlementRepository extends JpaRepository<PlanEntitlement, Long> {

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM subscription_plan_entitlement
            WHERE plan_version_id = :planVersionId
            ORDER BY priority ASC, created_at ASC
            """)
    List<PlanEntitlement> findAllByPlanVersionOrderByPriorityAsc(@Param("planVersionId") Long planVersionId);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM subscription_plan_entitlement
            WHERE plan_version_id = :planVersionId
              AND entitlement_definition_id = :entitlementDefinitionId
            ORDER BY priority ASC, created_at ASC
            LIMIT 1
            """)
    Optional<PlanEntitlement> findByPlanVersionAndEntitlementDefinition(@Param("planVersionId") Long planVersionId,
                                                                        @Param("entitlementDefinitionId") Long entitlementDefinitionId);
}
