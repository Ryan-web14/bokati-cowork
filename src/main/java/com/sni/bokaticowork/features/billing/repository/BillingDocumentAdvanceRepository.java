package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentAdvance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BillingDocumentAdvanceRepository extends JpaRepository<BillingDocumentAdvance, Long> {
    Optional<BillingDocumentAdvance> findByDocument(BillingDocument document);
}