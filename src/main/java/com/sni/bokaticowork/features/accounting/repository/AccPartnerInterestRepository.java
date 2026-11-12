package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccPartnerInterest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccPartnerInterestRepository extends JpaRepository<AccPartnerInterest, Long> {

    java.util.List<AccPartnerInterest> findAllByPartnerCodeOrderByToDateDesc(String partnerCode);

    java.util.Optional<AccPartnerInterest> findByPartnerCodeAndToDate(String partnerCode, java.time.LocalDate toDate);
}
