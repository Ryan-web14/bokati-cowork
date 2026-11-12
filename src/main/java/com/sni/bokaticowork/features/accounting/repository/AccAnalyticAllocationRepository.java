package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccAnalyticAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccAnalyticAllocationRepository extends JpaRepository<AccAnalyticAllocation, Long> {

    java.util.List<AccAnalyticAllocation> findByEntryLineId(Long entryLineId);

    java.util.List<AccAnalyticAllocation> findByEntryId(Long entryId);

    java.util.List<AccAnalyticAllocation> findByAxisCodeAndSectionCode(String axisCode, String sectionCode);

    boolean existsByEntryId(Long entryId);
}
