package com.sni.bokaticowork.features.fund.repository;

import com.sni.bokaticowork.features.fund.model.FundRequestLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FundRequestLineRepository extends JpaRepository<FundRequestLine, Long> {

    java.util.List<FundRequestLine> findByRequestIdOrderByLineNoAsc(Long requestId);

    void deleteByRequestId(Long requestId);
}
