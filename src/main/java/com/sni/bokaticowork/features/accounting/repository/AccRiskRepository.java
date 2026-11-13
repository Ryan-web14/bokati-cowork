package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccRisk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AccRiskRepository extends JpaRepository<AccRisk, Long> {
    Optional<AccRisk> findByCode(String code);
    List<AccRisk> findAllByOrderByCodeAsc();
    List<AccRisk> findAllByStatus(String status);
}
