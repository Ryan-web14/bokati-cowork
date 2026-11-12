package com.sni.bokaticowork.features.fund.repository;

import com.sni.bokaticowork.features.fund.model.FundDisbursement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FundDisbursementRepository extends JpaRepository<FundDisbursement, Long> {

    java.util.List<FundDisbursement> findByRequestIdOrderByCreatedAtAsc(Long requestId);
}
