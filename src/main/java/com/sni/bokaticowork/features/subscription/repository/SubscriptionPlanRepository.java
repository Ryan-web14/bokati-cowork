package com.sni.bokaticowork.features.subscription.repository;

import com.sni.bokaticowork.features.subscription.subscription.model.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long>, JpaSpecificationExecutor<SubscriptionPlan> {

    @Query(nativeQuery = true, value = "SELECT EXISTS(SELECT 1 FROM subscription_plan WHERE lower(code) = lower(:code) AND deleted = false)")
    boolean existsByCodeIgnoreCase(@Param("code") String code);

    @Query(nativeQuery = true, value = "SELECT * FROM subscription_plan WHERE lower(code) = lower(:code) AND deleted = false")
    Optional<SubscriptionPlan> findByCodeIgnoreCase(@Param("code") String code);
}
