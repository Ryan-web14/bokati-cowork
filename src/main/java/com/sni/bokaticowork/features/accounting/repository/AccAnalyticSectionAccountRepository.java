package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccAnalyticSectionAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccAnalyticSectionAccountRepository extends JpaRepository<AccAnalyticSectionAccount, Long> {

    java.util.List<AccAnalyticSectionAccount> findByAxisCode(String axisCode);

    java.util.List<AccAnalyticSectionAccount> findByAxisCodeAndSectionCode(String axisCode, String sectionCode);
}
