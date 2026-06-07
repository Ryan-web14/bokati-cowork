package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentEditHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingDocumentEditHistoryRepository extends JpaRepository<BillingDocumentEditHistory, Long> {
    List<BillingDocumentEditHistory> findAllByDocumentOrderByChangedAtDesc(BillingDocument document);
}
