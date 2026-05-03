package com.sni.bokaticowork.features.document.documentMaster.repository;

import com.sni.bokaticowork.features.document.documentMaster.model.DocumentSignature;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSignatureStatus;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentSignatureRepository extends JpaRepository<DocumentSignature, Long> {
    List<DocumentSignature> findAllByDocument_CodeOrderByIdAsc(String documentCode);
    List<DocumentSignature> findAllByDocumentAndSignatureStatus(Document document, DocumentSignatureStatus signatureStatus);
    Optional<DocumentSignature> findFirstByDocument_CodeAndSignerTypeAndSignerIdOrderByIdDesc(String documentCode, String signerType, Long signerId);
}
