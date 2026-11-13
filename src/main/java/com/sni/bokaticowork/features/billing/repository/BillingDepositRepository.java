package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.BillingDeposit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillingDepositRepository extends JpaRepository<BillingDeposit, Long> {

    List<BillingDeposit> findByDocumentNumberOrderByRankAsc(String documentNumber);

    List<BillingDeposit> findByFinalDocumentNumberOrderByRankAsc(String finalDocumentNumber);

    Optional<BillingDeposit> findFirstByDepositInvoiceNumber(String depositInvoiceNumber);
}
