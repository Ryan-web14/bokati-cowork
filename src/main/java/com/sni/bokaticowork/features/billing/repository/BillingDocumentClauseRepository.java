package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentClause;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingDocumentClauseRepository extends JpaRepository<BillingDocumentClause, Long> {
    List<BillingDocumentClause> findAllByDocumentOrderByDisplayOrderAscIdAsc(BillingDocument document);
}
