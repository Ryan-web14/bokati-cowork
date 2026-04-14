package com.sni.bokaticowork.features.document.documentMaster.repository;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRequirementRepository extends JpaRepository<DocumentRequirement, Long> {

    boolean existsByOwnerTypeAndDocumentTypeCodeAndCustomerTypeAndBusinessLegalForm(
            DocumentOwnerType ownerType,
            String documentTypeCode,
            String customerType,
            String businessLegalForm
    );

    List<DocumentRequirement> findAllByOwnerTypeOrderByDocumentTypeNameAsc(DocumentOwnerType ownerType);

    List<DocumentRequirement> findAllByOwnerTypeAndActiveTrueOrderByDocumentTypeNameAsc(DocumentOwnerType ownerType);
}
