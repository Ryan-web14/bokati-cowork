package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccReceivableImpairment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AccReceivableImpairmentRepository extends JpaRepository<AccReceivableImpairment, Long> {
    List<AccReceivableImpairment> findAllByDocumentNumberOrderByCreatedAtAsc(String documentNumber);
    List<AccReceivableImpairment> findAllByPeriodCode(String periodCode);
}
