package com.sni.bokaticowork.features.subscription.repository;

import com.sni.bokaticowork.features.subscription.subscription.model.PlanPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanPriceRepository extends JpaRepository<PlanPrice, Long> {

    @Query(nativeQuery = true, value = "SELECT * FROM subscription_plan_price WHERE plan_version_id = :planVersionId ORDER BY created_at ASC")
    List<PlanPrice> findAllByPlanVersion(@Param("planVersionId") Long planVersionId);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM subscription_plan_price
            WHERE plan_version_id = :planVersionId AND billing_cycle = :billingCycle
            ORDER BY created_at ASC
            LIMIT 1
            """)
    Optional<PlanPrice> findFirstByPlanVersionAndBillingCycle(@Param("planVersionId") Long planVersionId, @Param("billingCycle") String billingCycle);
}
