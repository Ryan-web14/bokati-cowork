package com.sni.bokaticowork.features.fund.repository;

import com.sni.bokaticowork.features.fund.model.FundApprovalDelegation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FundApprovalDelegationRepository extends JpaRepository<FundApprovalDelegation, Long> {

    java.util.List<FundApprovalDelegation> findByCancelledAtIsNullOrderByStartsOnAsc();
}
