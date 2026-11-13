package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccConfirmationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccConfirmationRequestRepository extends JpaRepository<AccConfirmationRequest, Long> {
    List<AccConfirmationRequest> findAllByOrderByCreatedAtDesc();
    java.util.Optional<AccConfirmationRequest> findByNumber(String number);
    boolean existsByAccountNumberAndThirdPartyCodeAndBalanceDate(String accountNumber, String thirdPartyCode, java.time.LocalDate balanceDate);
}
