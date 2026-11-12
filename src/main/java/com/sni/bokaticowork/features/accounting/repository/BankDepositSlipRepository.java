package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.BankDepositSlip;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankDepositSlipRepository extends JpaRepository<BankDepositSlip, Long> {

    java.util.Optional<BankDepositSlip> findByNumber(String number);

    java.util.List<BankDepositSlip> findAllByOrderByCreatedAtDesc();
}
