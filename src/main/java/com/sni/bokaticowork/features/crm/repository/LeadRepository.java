package com.sni.bokaticowork.features.crm.repository;

import com.sni.bokaticowork.features.crm.enums.LeadStage;
import com.sni.bokaticowork.features.crm.model.Lead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeadRepository extends JpaRepository<Lead, Long> {
    Page<Lead> findAllByStage(LeadStage stage, Pageable pageable);
    List<Lead> findAllByStage(LeadStage stage);
}
