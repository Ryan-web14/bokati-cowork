package com.sni.bokaticowork.features.document.kyc.repository;

import com.sni.bokaticowork.features.document.kyc.model.KycDocument;
import com.sni.bokaticowork.features.document.kyc.model.KycVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KycVerificationRepository extends JpaRepository<KycVerification, Long> {

    List<KycVerification> findAllByKycDocumentOrderByVerifiedAtDesc(KycDocument kycDocument);
}
