package com.sni.bokaticowork.features.ressource.repository.repo;

import com.sni.bokaticowork.features.ressource.model.ResourcePricingRuleVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourcePricingRuleVersionRepository extends JpaRepository<ResourcePricingRuleVersion, Long> {

    List<ResourcePricingRuleVersion> findAllByRuleShortIdOrderByVersionDesc(Long ruleShortId);

    long countByRuleShortId(Long ruleShortId);
}
