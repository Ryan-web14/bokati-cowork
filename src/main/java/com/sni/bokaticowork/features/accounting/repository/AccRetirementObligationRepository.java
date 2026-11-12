package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccRetirementObligation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AccRetirementObligationRepository extends JpaRepository<AccRetirementObligation, Long> {
    List<AccRetirementObligation> findAllByOrderByCreatedAtDesc();
}
