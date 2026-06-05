package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentSignature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BillingDocumentSignatureRepository extends JpaRepository<BillingDocumentSignature, Long> {

    @Query("SELECT s FROM BillingDocumentSignature s JOIN FETCH s.document WHERE s.signatureToken = :token")
    Optional<BillingDocumentSignature> findBySignatureTokenWithDocument(@Param("token") String token);

    Optional<BillingDocumentSignature> findBySignatureToken(String token);
    Optional<BillingDocumentSignature> findFirstByDocumentOrderByCreatedAtDesc(BillingDocument document);
}
