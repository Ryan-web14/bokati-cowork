package com.sni.bokaticowork.features.booking.repository;

import com.sni.bokaticowork.features.booking.model.CancellationPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CancellationPolicyRepository extends JpaRepository<CancellationPolicy, Long> {

    List<CancellationPolicy> findAllByActiveTrueOrderByRuleOrderAsc();
}
