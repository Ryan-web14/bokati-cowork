package com.sni.bokaticowork.features.document.kyc.repository;

import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.model.KycCaseRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KycCaseRequirementRepository extends JpaRepository<KycCaseRequirement, Long> {

    List<KycCaseRequirement> findAllByKycCaseAndActiveTrueOrderByDocumentTypeNameAsc(KycCase kycCase);

    List<KycCaseRequirement> findAllByKycCaseOrderByDocumentTypeNameAsc(KycCase kycCase);

    Optional<KycCaseRequirement> findByKycCaseAndDocumentTypeCode(KycCase kycCase, String documentTypeCode);

    boolean existsByKycCaseAndDocumentTypeCode(KycCase kycCase, String documentTypeCode);
}
