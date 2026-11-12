package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccPayrollLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccPayrollLineRepository extends JpaRepository<AccPayrollLine, Long> {

    java.util.List<AccPayrollLine> findAllByImportIdOrderByLineNoAsc(Long importId);

    void deleteAllByImportId(Long importId);
}
