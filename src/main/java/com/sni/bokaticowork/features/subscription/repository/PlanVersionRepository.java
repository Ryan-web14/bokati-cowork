package com.sni.bokaticowork.features.subscription.repository;

import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanVersionRepository extends JpaRepository<PlanVersion, Long> {

    @Query(nativeQuery = true, value = "SELECT * FROM subscription_plan_version WHERE plan_id = :planId ORDER BY version_number DESC")
    List<PlanVersion> findAllByPlanOrderByVersionNumberDesc(@Param("planId") Long planId);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM subscription_plan_version
            WHERE plan_id = :planId AND status = :status
            ORDER BY version_number DESC
            LIMIT 1
            """)
    Optional<PlanVersion> findFirstByPlanAndStatusOrderByVersionNumberDesc(@Param("planId") Long planId, @Param("status") String status);

    @Query(nativeQuery = true, value = "SELECT COALESCE(MAX(version_number), 0) FROM subscription_plan_version WHERE plan_id = :planId")
    Integer maxVersionNumber(@Param("planId") Long planId);
}
