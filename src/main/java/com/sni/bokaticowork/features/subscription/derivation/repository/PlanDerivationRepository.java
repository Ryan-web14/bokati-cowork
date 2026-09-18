package com.sni.bokaticowork.features.subscription.derivation.repository;

import com.sni.bokaticowork.features.subscription.derivation.model.PlanDerivation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlanDerivationRepository extends JpaRepository<PlanDerivation, Long> {

    Optional<PlanDerivation> findByDerivationCode(String derivationCode);

    Optional<PlanDerivation> findFirstBySubscription_IdAndStatus(Long subscriptionId, PlanDerivation.Status status);

    List<PlanDerivation> findBySubscription_IdOrderByCreatedAtDesc(Long subscriptionId);

    Page<PlanDerivation> findByStatusInOrderByCreatedAtDesc(Collection<PlanDerivation.Status> statuses, Pageable pageable);

    Page<PlanDerivation> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<PlanDerivation> findByBatchCodeOrderByCreatedAtAsc(String batchCode);

    List<PlanDerivation> findByStatus(PlanDerivation.Status status);

    /** Les concessions par demandeur sur une periode · ce que coute chaque commercial. */
    @Query(nativeQuery = true, value = """
            SELECT requested_by, reason, COUNT(*), COALESCE(SUM(total_impact_amount), 0)
            FROM plan_derivation
            WHERE status IN ('ACTIVE', 'SUPERSEDED', 'EXPIRED') AND applied_at >= :from AND applied_at < :to
            GROUP BY requested_by, reason
            ORDER BY SUM(total_impact_amount) DESC
            """)
    List<Object[]> concessionsBetween(@Param("from") Instant from, @Param("to") Instant to);
}
