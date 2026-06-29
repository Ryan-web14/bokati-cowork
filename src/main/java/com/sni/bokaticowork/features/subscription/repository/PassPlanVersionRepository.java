package com.sni.bokaticowork.features.subscription.repository;

import com.sni.bokaticowork.features.subscription.subscription.enums.PlanStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.PassPlan;
import com.sni.bokaticowork.features.subscription.subscription.model.PassPlanVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PassPlanVersionRepository extends JpaRepository<PassPlanVersion, Long> {

    Optional<PassPlanVersion> findFirstByPlanAndStatusOrderByVersionNumberDesc(PassPlan plan, PlanStatus status);

    List<PassPlanVersion> findAllByPlanOrderByVersionNumberDesc(PassPlan plan);

    int countByPlan(PassPlan plan);
}
