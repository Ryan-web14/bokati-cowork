package com.sni.bokaticowork.features.subscription.repository;

import com.sni.bokaticowork.features.subscription.subscription.model.PlanBenefit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanBenefitRepository extends JpaRepository<PlanBenefit, Long> {

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM subscription_plan_benefit
            WHERE plan_version_id = :planVersionId
            ORDER BY display_order ASC NULLS LAST, created_at ASC
            """)
    List<PlanBenefit> findAllByPlanVersionOrderByDisplayOrderAscCreatedAtAsc(@Param("planVersionId") Long planVersionId);
}
