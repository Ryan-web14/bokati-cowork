package com.sni.bokaticowork.features.document.documentMaster.repository;

import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentShareLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentShareLinkRepository extends JpaRepository<DocumentShareLink, Long> {
    Optional<DocumentShareLink> findByTokenAndActiveTrue(String token);
    List<DocumentShareLink> findAllByDocumentAndActiveTrueOrderByCreatedAtDesc(Document document);
    List<DocumentShareLink> findAllByCreatedByAndActiveTrueOrderByCreatedAtDesc(Long createdBy);
}
