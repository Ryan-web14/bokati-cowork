package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.BalanceJustification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BalanceJustificationRepository extends JpaRepository<BalanceJustification, Long> {
    List<BalanceJustification> findAllByAccountNumberAndJustifiedAtOrderByPreparedAtDesc(String accountNumber, java.time.LocalDate justifiedAt);
}
