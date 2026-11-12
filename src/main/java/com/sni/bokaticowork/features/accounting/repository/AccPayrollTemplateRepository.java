package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccPayrollTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccPayrollTemplateRepository extends JpaRepository<AccPayrollTemplate, String> {

    java.util.List<AccPayrollTemplate> findAllByOrderByCodeAsc();
}
