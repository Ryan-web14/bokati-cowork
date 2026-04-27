package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentDiscount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingDocumentDiscountRepository extends JpaRepository<BillingDocumentDiscount, Long> {
    List<BillingDocumentDiscount> findAllByDocumentOrderByIdAsc(BillingDocument document);
}
