package com.sni.bokaticowork.features.document.kyc.repository;

import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.kyc.KycDocumentVerificationStatus;
import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.model.KycDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {

    List<KycDocument> findAllByKycCaseOrderByIdAsc(KycCase kycCase);

    Optional<KycDocument> findByKycCaseAndDocument(KycCase kycCase, Document document);

    Optional<KycDocument> findByDocument(Document document);

    List<KycDocument> findAllByDocument(Document document);

    Optional<KycDocument> findByDocument_Code(String documentCode);

    List<KycDocument> findAllByDocument_Code(String documentCode);

    List<KycDocument> findAllByExpiryDateBetween(LocalDate start, LocalDate end);

    List<KycDocument> findAllByExpiryDateBeforeAndStatusNot(LocalDate date, KycDocumentVerificationStatus status);

    List<KycDocument> findAllByStatusIn(Collection<KycDocumentVerificationStatus> statuses);
}
