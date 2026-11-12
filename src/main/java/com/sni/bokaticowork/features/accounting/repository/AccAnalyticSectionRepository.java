package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccAnalyticSection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccAnalyticSectionRepository extends JpaRepository<AccAnalyticSection, Long> {

    java.util.Optional<AccAnalyticSection> findByAxisCodeAndCode(String axisCode, String code);

    java.util.List<AccAnalyticSection> findByAxisCodeOrderBySortOrderAscCodeAsc(String axisCode);

    java.util.List<AccAnalyticSection> findByStatusOrderByCreatedAtAsc(String status);
}
