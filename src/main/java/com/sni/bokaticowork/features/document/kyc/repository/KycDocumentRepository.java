package com.sni.bokaticowork.features.document.kyc.repository;

import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.kyc.KycDocumentVerificationStatus;
import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.model.KycDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            select kd
            from KycDocument kd
            join fetch kd.document d
            where kd.status in :statuses
            order by d.updatedAt desc, kd.id desc
            """)
    List<KycDocument> findAllReviewDocumentsByStatusIn(@Param("statuses") Collection<KycDocumentVerificationStatus> statuses);

    @Query("""
            select kd
            from KycDocument kd
            join fetch kd.document d
            where kd.kycCase = :kycCase
              and kd.status in :statuses
            order by d.updatedAt desc, kd.id desc
            """)
    List<KycDocument> findAllReviewDocumentsByKycCaseAndStatusIn(
            @Param("kycCase") KycCase kycCase,
            @Param("statuses") Collection<KycDocumentVerificationStatus> statuses);

    Optional<KycDocument> findByKycCaseAndDocumentType(KycCase kycCase, String documentType);
}
