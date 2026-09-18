package com.sni.bokaticowork.features.subscription.derivation.repository;

import com.sni.bokaticowork.features.subscription.derivation.model.PlanDerivationDelta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanDerivationDeltaRepository extends JpaRepository<PlanDerivationDelta, Long> {

    List<PlanDerivationDelta> findByDerivation_IdOrderByIdAsc(Long derivationId);
}
