package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentEarlyPaymentDiscount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BillingDocumentEarlyPaymentDiscountRepository extends JpaRepository<BillingDocumentEarlyPaymentDiscount, Long> {
    Optional<BillingDocumentEarlyPaymentDiscount> findByDocument(BillingDocument document);
}
