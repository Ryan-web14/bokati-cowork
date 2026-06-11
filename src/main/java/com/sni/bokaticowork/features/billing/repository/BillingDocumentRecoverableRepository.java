package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentRecoverable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillingDocumentRecoverableRepository extends JpaRepository<BillingDocumentRecoverable, Long> {

    List<BillingDocumentRecoverable> findAllByDocumentOrderByCreatedAtAsc(BillingDocument document);

    List<BillingDocumentRecoverable> findAllByDocumentAndStatusOrderByCreatedAtAsc(BillingDocument document, String status);

    Optional<BillingDocumentRecoverable> findByRecoverableNumber(String recoverableNumber);
}
