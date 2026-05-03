package com.sni.bokaticowork.features.document.kyc.repository;

import com.sni.bokaticowork.features.document.kyc.model.KycCrossValidationRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KycCrossValidationRuleRepository extends JpaRepository<KycCrossValidationRule, Long> {
    List<KycCrossValidationRule> findAllByActiveTrueOrderByIdAsc();
}
