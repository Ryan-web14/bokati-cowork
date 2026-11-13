package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.BillingBatchRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Lot C13 : lancements de facturation groupee. */
@Repository
public interface BillingBatchRunRepository extends JpaRepository<BillingBatchRun, Long> {

    Optional<BillingBatchRun> findByRunNumber(String runNumber);

    List<BillingBatchRun> findTop50ByOrderByCreatedAtDesc();

    List<BillingBatchRun> findByStatus(String status);
}
