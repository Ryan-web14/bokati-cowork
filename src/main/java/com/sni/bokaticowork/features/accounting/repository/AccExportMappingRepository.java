package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccExportMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccExportMappingRepository extends JpaRepository<AccExportMapping, Long> {
    List<AccExportMapping> findAllByFormatOrderByKindAscSourceAsc(String format);
    java.util.Optional<AccExportMapping> findByFormatAndKindAndSource(String format, String kind, String source);
}
