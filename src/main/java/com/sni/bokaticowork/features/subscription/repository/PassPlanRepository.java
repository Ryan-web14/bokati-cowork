package com.sni.bokaticowork.features.subscription.repository;

import com.sni.bokaticowork.features.subscription.subscription.model.PassPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PassPlanRepository extends JpaRepository<PassPlan, Long>, JpaSpecificationExecutor<PassPlan> {

    Optional<PassPlan> findByCode(String code);
}
