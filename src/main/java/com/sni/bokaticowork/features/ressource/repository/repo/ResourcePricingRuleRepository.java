package com.sni.bokaticowork.features.ressource.repository.repo;

import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourcePricingRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResourcePricingRuleRepository extends JpaRepository<ResourcePricingRule, Long> {

    Page<ResourcePricingRule> findAllByResource(Resource resource, Pageable pageable);

    Optional<ResourcePricingRule> findByIdAndResource(Long id, Resource resource);
}
