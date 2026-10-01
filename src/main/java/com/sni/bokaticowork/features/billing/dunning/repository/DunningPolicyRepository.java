package com.sni.bokaticowork.features.billing.dunning.repository;

import com.sni.bokaticowork.features.billing.dunning.model.DunningPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DunningPolicyRepository extends JpaRepository<DunningPolicy, Long> {

    Optional<DunningPolicy> findByPolicyCode(String policyCode);

    Optional<DunningPolicy> findFirstBySegmentAndActiveTrue(DunningPolicy.Segment segment);

    List<DunningPolicy> findAllByOrderBySegmentAsc();

    boolean existsByActiveTrue();
}
