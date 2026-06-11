package com.sni.bokaticowork.features.crm.repository;

import com.sni.bokaticowork.features.crm.model.Lead;
import com.sni.bokaticowork.features.crm.model.LeadActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LeadActivityRepository extends JpaRepository<LeadActivity, Long> {
    List<LeadActivity> findAllByLeadOrderByPerformedAtDesc(Lead lead);

    @Query("SELECT a.lead.id, COUNT(a) FROM LeadActivity a WHERE a.lead.id IN :leadIds GROUP BY a.lead.id")
    List<Object[]> countByLeadIds(@Param("leadIds") List<Long> leadIds);
}
