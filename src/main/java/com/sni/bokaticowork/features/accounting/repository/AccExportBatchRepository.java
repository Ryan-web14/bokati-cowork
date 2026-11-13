package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccExportBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccExportBatchRepository extends JpaRepository<AccExportBatch, Long> {
    List<AccExportBatch> findTop100ByOrderByCreatedAtDesc();
    long countByFormat(String format);
}
