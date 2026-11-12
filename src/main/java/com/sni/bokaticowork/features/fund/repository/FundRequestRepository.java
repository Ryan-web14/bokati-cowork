package com.sni.bokaticowork.features.fund.repository;

import com.sni.bokaticowork.features.fund.model.FundRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FundRequestRepository extends JpaRepository<FundRequest, Long> {

    java.util.Optional<FundRequest> findByNumber(String number);

    java.util.List<FundRequest> findByRequesterOrBeneficiaryCodeOrderByCreatedAtDesc(String requester, String beneficiaryCode);

    java.util.List<FundRequest> findByStatusInOrderByCreatedAtAsc(java.util.Collection<com.sni.bokaticowork.features.fund.enums.FundRequestStatus> statuses);

    java.util.List<FundRequest> findByBeneficiaryCodeAndStatusIn(String beneficiaryCode, java.util.Collection<com.sni.bokaticowork.features.fund.enums.FundRequestStatus> statuses);

    java.util.Optional<FundRequest> findByLegacyRequestNumber(String legacyRequestNumber);
}
