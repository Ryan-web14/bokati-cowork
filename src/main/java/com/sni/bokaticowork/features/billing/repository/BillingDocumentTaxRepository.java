package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentTax;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingDocumentTaxRepository extends JpaRepository<BillingDocumentTax, Long> {
    List<BillingDocumentTax> findAllByDocumentOrderByIdAsc(BillingDocument document);
    void deleteAllByDocument(BillingDocument document);
}
