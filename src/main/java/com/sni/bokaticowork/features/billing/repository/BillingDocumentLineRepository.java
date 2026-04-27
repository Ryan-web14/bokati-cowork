package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingDocumentLineRepository extends JpaRepository<BillingDocumentLine, Long> {
    List<BillingDocumentLine> findAllByDocumentOrderByLineOrderAscIdAsc(BillingDocument document);
}
