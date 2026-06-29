package com.sni.bokaticowork.features.subscription.repository;

import com.sni.bokaticowork.features.subscription.subscription.model.PassPlanEntitlement;
import com.sni.bokaticowork.features.subscription.subscription.model.PassPlanVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PassPlanEntitlementRepository extends JpaRepository<PassPlanEntitlement, Long> {

    List<PassPlanEntitlement> findAllByPassVersion(PassPlanVersion version);
}
