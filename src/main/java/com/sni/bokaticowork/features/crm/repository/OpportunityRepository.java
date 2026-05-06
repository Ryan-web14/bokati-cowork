package com.sni.bokaticowork.features.crm.repository;

import com.sni.bokaticowork.features.crm.enums.OpportunityStage;
import com.sni.bokaticowork.features.crm.model.Lead;
import com.sni.bokaticowork.features.crm.model.Opportunity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface OpportunityRepository extends JpaRepository<Opportunity, Long> {

    Optional<Opportunity> findByOpportunityNumber(String opportunityNumber);

    List<Opportunity> findAllByLeadOrderByCreatedAtDesc(Lead lead);

    Page<Opportunity> findAllByStage(OpportunityStage stage, Pageable pageable);

    Page<Opportunity> findAll(Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(o.estimatedAmount), 0)
            FROM Opportunity o
            WHERE o.stage NOT IN ('WON','LOST')
              AND o.estimatedAmount IS NOT NULL
            """)
    BigDecimal sumActivePipelineValue();
}
