package com.sni.bokaticowork.features.document.documentMaster.repository;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentTypeRepository extends JpaRepository<DocumentType, Long> {

    Optional<DocumentType> findByCode(String code);

    boolean existsByCode(String code);

    List<DocumentType> findAllByOrderByNameAsc();

    List<DocumentType> findAllByActiveTrueOrderByNameAsc();

    List<DocumentType> findAllByOwnerTypeOrderByNameAsc(DocumentOwnerType ownerType);

    List<DocumentType> findAllByOwnerTypeAndActiveTrueOrderByNameAsc(DocumentOwnerType ownerType);
}
