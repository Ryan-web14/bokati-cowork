package com.sni.bokaticowork.features.fund.repository;

import com.sni.bokaticowork.features.fund.model.FundNeedStatement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FundNeedStatementRepository extends JpaRepository<FundNeedStatement, Long> {

    java.util.Optional<FundNeedStatement> findByNumber(String number);

    java.util.List<FundNeedStatement> findByRequesterOrderByCreatedAtDesc(String requester);

    java.util.List<FundNeedStatement> findByStatusOrderByCreatedAtAsc(com.sni.bokaticowork.features.fund.enums.FundNeedStatus status);
}
