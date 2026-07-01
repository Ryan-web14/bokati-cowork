package com.sni.bokaticowork.features.contract.repository;

import com.sni.bokaticowork.features.contract.model.ContractTemplateSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractTemplateSectionRepository extends JpaRepository<ContractTemplateSection, Long> {

    List<ContractTemplateSection> findAllByTemplateCodeOrderBySectionOrderAsc(String templateCode);

    Optional<ContractTemplateSection> findByIdAndTemplateCode(Long id, String templateCode);
}
