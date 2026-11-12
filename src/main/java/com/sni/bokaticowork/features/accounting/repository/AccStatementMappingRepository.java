package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccStatementMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccStatementMappingRepository extends JpaRepository<AccStatementMapping, Long> {
    List<AccStatementMapping> findAllByTemplateId(Long templateId);
}
