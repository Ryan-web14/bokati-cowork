package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.BillingBatchRunItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/** Lot C13 : clients des lancements de facturation groupee. */
@Repository
public interface BillingBatchRunItemRepository extends JpaRepository<BillingBatchRunItem, Long> {

    List<BillingBatchRunItem> findByRunIdOrderByCustomerNameAsc(Long runId);

    List<BillingBatchRunItem> findByRunIdIn(Collection<Long> runIds);
}
