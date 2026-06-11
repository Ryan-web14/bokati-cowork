package com.sni.bokaticowork.features.document.documentMaster.repository;

import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentTag;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentTagAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentTagAssignmentRepository extends JpaRepository<DocumentTagAssignment, Long> {

    List<DocumentTagAssignment> findAllByDocument(Document document);

    Optional<DocumentTagAssignment> findByDocumentAndTag(Document document, DocumentTag tag);

    boolean existsByDocumentAndTag(Document document, DocumentTag tag);

    boolean existsByTag(DocumentTag tag);

    void deleteByDocumentAndTag(Document document, DocumentTag tag);
}
