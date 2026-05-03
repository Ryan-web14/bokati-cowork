package com.sni.bokaticowork.features.crm.repository;

import com.sni.bokaticowork.features.crm.model.Lead;
import com.sni.bokaticowork.features.crm.model.LeadActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeadActivityRepository extends JpaRepository<LeadActivity, Long> {
    List<LeadActivity> findAllByLeadOrderByPerformedAtDesc(Lead lead);
}
