package com.sni.bokaticowork.features.document.documentMaster.repository;

import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentMetadataRepository extends JpaRepository<DocumentMetadata, Long> {

    List<DocumentMetadata> findAllByDocument(Document document);

    Optional<DocumentMetadata> findByDocumentAndMetaKey(Document document, String metaKey);

    void deleteAllByDocument(Document document);
}
