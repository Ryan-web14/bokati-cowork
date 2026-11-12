package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.BankCheque;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankChequeRepository extends JpaRepository<BankCheque, Long> {

    java.util.Optional<BankCheque> findByTransactionNumber(String transactionNumber);

    java.util.List<BankCheque> findByStatusOrderByReceivedAtAsc(String status);

    java.util.List<BankCheque> findBySlipNumber(String slipNumber);
}
