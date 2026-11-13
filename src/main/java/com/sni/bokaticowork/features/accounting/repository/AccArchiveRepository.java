package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccArchive;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccArchiveRepository extends JpaRepository<AccArchive, Long> {
    List<AccArchive> findAllByFiscalYearCodeOrderByCreatedAtDesc(String fiscalYearCode);
}
