package com.sni.bokaticowork.features.subscription.lifecycle.repository;

import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionPolicyRepository extends JpaRepository<SubscriptionPolicy, Long> {

    Optional<SubscriptionPolicy> findFirstByActiveTrueOrderByIdAsc();

    Optional<SubscriptionPolicy> findByPolicyCode(String policyCode);
}
