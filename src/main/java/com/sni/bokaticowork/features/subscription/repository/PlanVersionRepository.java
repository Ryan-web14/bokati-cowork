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

    // Les versions privees (scope SUBSCRIPTION) n'apparaissent jamais au catalogue · c'est la seule
    // ligne qui change pour tout le module, et c'est voulu : le reste les lit comme les autres.
    @Query(nativeQuery = true, value = "SELECT * FROM subscription_plan_version WHERE plan_id = :planId AND scope = 'CATALOGUE' ORDER BY version_number DESC")
    List<PlanVersion> findAllByPlanOrderByVersionNumberDesc(@Param("planId") Long planId);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM subscription_plan_version
            WHERE plan_id = :planId AND status = :status AND scope = 'CATALOGUE'
            ORDER BY version_number DESC
            LIMIT 1
            """)
    Optional<PlanVersion> findFirstByPlanAndStatusOrderByVersionNumberDesc(@Param("planId") Long planId, @Param("status") String status);

    @Query(nativeQuery = true, value = "SELECT COALESCE(MAX(version_number), 0) FROM subscription_plan_version WHERE plan_id = :planId")
    Integer maxVersionNumber(@Param("planId") Long planId);

    /** Les versions privees d'un abonnement · pour retrouver ce qu'on lui a fait. */
    @Query(nativeQuery = true, value = "SELECT * FROM subscription_plan_version WHERE owner_subscription_id = :subscriptionId ORDER BY version_number DESC")
    List<PlanVersion> findPrivateVersions(@Param("subscriptionId") Long subscriptionId);
}
