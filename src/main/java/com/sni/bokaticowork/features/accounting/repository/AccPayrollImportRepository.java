package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccPayrollImport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccPayrollImportRepository extends JpaRepository<AccPayrollImport, Long> {

    java.util.List<AccPayrollImport> findTop36ByOrderByImportedAtDesc();

    java.util.List<AccPayrollImport> findAllByPeriodCodeAndStatusNot(String periodCode, String status);
}
