package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccAnalyticAxis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccAnalyticAxisRepository extends JpaRepository<AccAnalyticAxis, Long> {

    java.util.Optional<AccAnalyticAxis> findByCode(String code);

    java.util.List<AccAnalyticAxis> findByActiveTrueOrderBySortOrderAsc();

    java.util.List<AccAnalyticAxis> findAllByOrderBySortOrderAsc();
}
