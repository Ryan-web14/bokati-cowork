package com.sni.bokaticowork.features.document.documentMaster.repository;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentStatus;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Document> {

    Optional<Document> findByCode(String code);

    boolean existsByCode(String code);

    List<Document> findAllByOwnerTypeAndOwnerId(DocumentOwnerType ownerType, Long ownerId);

    Page<Document> findAllByOwnerTypeAndOwnerId(DocumentOwnerType ownerType, Long ownerId, Pageable pageable);

    List<Document> findAllByStatusInAndExpiryDateBefore(List<DocumentStatus> statuses, LocalDate date);

    long countByOwnerTypeAndOwnerIdAndDocumentTypeAndStatusNotIn(
            DocumentOwnerType ownerType, Long ownerId, DocumentType documentType, Collection<DocumentStatus> excludedStatuses);

    Page<Document> findAllBySpaceAndStatus(DocumentSpace space, DocumentStatus status, Pageable pageable);

    Page<Document> findAllByStatus(DocumentStatus status, Pageable pageable);

    List<Document> findAllByStatusInAndExpiryDateBetween(List<DocumentStatus> statuses, LocalDate from, LocalDate to);
}
