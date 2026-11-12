package com.sni.bokaticowork.features.billing.dunning.repository;

import com.sni.bokaticowork.features.billing.dunning.model.DunningPolicy;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DunningPolicyRepository extends JpaRepository<DunningPolicy, Long> {

    /** Les etapes sont lues avec la politique : sa vue les lit hors transaction. */
    @EntityGraph(attributePaths = "steps")
    Optional<DunningPolicy> findByPolicyCode(String policyCode);

    Optional<DunningPolicy> findFirstBySegmentAndActiveTrue(DunningPolicy.Segment segment);

    @EntityGraph(attributePaths = "steps")
    List<DunningPolicy> findAllByOrderBySegmentAsc();

    boolean existsByActiveTrue();
}
