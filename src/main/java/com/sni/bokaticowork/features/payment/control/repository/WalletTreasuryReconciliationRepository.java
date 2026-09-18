package com.sni.bokaticowork.features.payment.control.repository;

import com.sni.bokaticowork.features.payment.control.model.WalletTreasuryReconciliation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface WalletTreasuryReconciliationRepository extends JpaRepository<WalletTreasuryReconciliation, Long> {

    Optional<WalletTreasuryReconciliation> findByReconciliationNumber(String number);

    Optional<WalletTreasuryReconciliation> findByReconciliationDateAndCurrency(LocalDate date, String currency);

    Page<WalletTreasuryReconciliation> findAllByOrderByReconciliationDateDesc(Pageable pageable);

    Optional<WalletTreasuryReconciliation> findFirstByOrderByReconciliationDateDesc();
}
