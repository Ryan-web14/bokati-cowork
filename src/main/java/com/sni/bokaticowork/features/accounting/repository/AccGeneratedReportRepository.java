package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccGeneratedReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccGeneratedReportRepository extends JpaRepository<AccGeneratedReport, Long> {
    List<AccGeneratedReport> findAllByFiscalYearCodeOrderByGeneratedAtDesc(String fiscalYearCode);
    java.util.Optional<AccGeneratedReport> findByNumber(String number);
    long countByFiscalYearCode(String fiscalYearCode);
}
