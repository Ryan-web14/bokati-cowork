package com.sni.bokaticowork.features.document.template.repository;

import com.sni.bokaticowork.features.document.template.model.DocumentTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentTemplateRepository extends JpaRepository<DocumentTemplate, Long> {

    Optional<DocumentTemplate> findByCode(String code);

    List<DocumentTemplate> findAllByOrderByFamilyAscDomainAscLabelAsc();
}
