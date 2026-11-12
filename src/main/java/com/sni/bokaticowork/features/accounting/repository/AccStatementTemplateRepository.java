package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccStatementTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccStatementTemplateRepository extends JpaRepository<AccStatementTemplate, Long> {
    java.util.Optional<AccStatementTemplate> findFirstByCodeAndActiveTrueOrderByVersionDesc(String code);
}
