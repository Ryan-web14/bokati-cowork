package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentSignature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BillingDocumentSignatureRepository extends JpaRepository<BillingDocumentSignature, Long> {
    Optional<BillingDocumentSignature> findBySignatureToken(String token);
    Optional<BillingDocumentSignature> findFirstByDocumentOrderByCreatedAtDesc(BillingDocument document);
}
