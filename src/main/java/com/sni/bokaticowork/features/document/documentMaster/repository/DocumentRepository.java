package com.sni.bokaticowork.features.document.documentMaster.repository;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByCode(String code);

    boolean existsByCode(String code);

    List<Document> findAllByOwnerTypeAndOwnerId(DocumentOwnerType ownerType, Long ownerId);

    Page<Document> findAllByOwnerTypeAndOwnerId(DocumentOwnerType ownerType, Long ownerId, Pageable pageable);

    List<Document> findAllByStatusInAndExpiryDateBefore(List<com.sni.bokaticowork.features.document.documentMaster.enums.DocumentStatus> statuses, LocalDate date);
}
