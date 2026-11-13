package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccRiskProvisionMove;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AccRiskProvisionMoveRepository extends JpaRepository<AccRiskProvisionMove, Long> {
    List<AccRiskProvisionMove> findAllByRiskIdOrderByMoveDateAscIdAsc(Long riskId);
    List<AccRiskProvisionMove> findAllByMoveDateBetween(LocalDate from, LocalDate to);
}
