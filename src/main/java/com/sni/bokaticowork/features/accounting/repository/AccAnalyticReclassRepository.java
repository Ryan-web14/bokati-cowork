package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccAnalyticReclass;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccAnalyticReclassRepository extends JpaRepository<AccAnalyticReclass, Long> {

    java.util.List<AccAnalyticReclass> findByEntryLineIdOrderByCreatedAtAsc(Long entryLineId);
}
