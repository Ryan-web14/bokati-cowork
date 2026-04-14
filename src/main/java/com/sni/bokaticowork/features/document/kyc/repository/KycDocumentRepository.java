package com.sni.bokaticowork.features.document.kyc.repository;

import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.model.KycDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {

    List<KycDocument> findAllByKycCaseOrderByIdAsc(KycCase kycCase);

    Optional<KycDocument> findByKycCaseAndDocument(KycCase kycCase, Document document);

    Optional<KycDocument> findByDocument(Document document);
}
