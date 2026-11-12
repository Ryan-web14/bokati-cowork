package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccAnalyticDistribution;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccAnalyticDistributionRepository extends JpaRepository<AccAnalyticDistribution, Long> {

    java.util.List<AccAnalyticDistribution> findByPeriodCodeAndAxisCode(String periodCode, String axisCode);

    void deleteByPeriodCodeAndAxisCode(String periodCode, String axisCode);
}
