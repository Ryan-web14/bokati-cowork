package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccRiskValuation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AccRiskValuationRepository extends JpaRepository<AccRiskValuation, Long> {
    List<AccRiskValuation> findAllByRiskIdOrderByValuationDateAsc(Long riskId);
}
