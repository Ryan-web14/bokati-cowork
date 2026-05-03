package com.sni.bokaticowork.features.document.kyc.repository;

import com.sni.bokaticowork.features.document.kyc.model.KycDocument;
import com.sni.bokaticowork.features.document.kyc.model.KycDocumentOcrResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KycDocumentOcrResultRepository extends JpaRepository<KycDocumentOcrResult, Long> {
    Optional<KycDocumentOcrResult> findByKycDocument(KycDocument kycDocument);
}
