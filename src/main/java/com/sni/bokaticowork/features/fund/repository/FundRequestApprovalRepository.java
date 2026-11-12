package com.sni.bokaticowork.features.fund.repository;

import com.sni.bokaticowork.features.fund.model.FundRequestApproval;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FundRequestApprovalRepository extends JpaRepository<FundRequestApproval, Long> {

    java.util.List<FundRequestApproval> findByRequestIdOrderByDecidedAtAscIdAsc(Long requestId);
}
