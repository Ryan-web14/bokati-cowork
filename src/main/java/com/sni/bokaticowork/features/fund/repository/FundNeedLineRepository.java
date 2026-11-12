package com.sni.bokaticowork.features.fund.repository;

import com.sni.bokaticowork.features.fund.model.FundNeedLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FundNeedLineRepository extends JpaRepository<FundNeedLine, Long> {

    java.util.List<FundNeedLine> findByStatementIdOrderByLineNoAsc(Long statementId);

    void deleteByStatementId(Long statementId);
}
