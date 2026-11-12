package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.BankImportTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankImportTemplateRepository extends JpaRepository<BankImportTemplate, Long> {

    java.util.Optional<BankImportTemplate> findByCode(String code);
}
