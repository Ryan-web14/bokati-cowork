package com.sni.bokaticowork.features.contract.repository;

import com.sni.bokaticowork.features.contract.model.ContractAmendmentSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContractAmendmentSectionRepository extends JpaRepository<ContractAmendmentSection, Long> {
    List<ContractAmendmentSection> findAllByAmendmentCodeOrderBySectionOrderAsc(String amendmentCode);
    Optional<ContractAmendmentSection> findByIdAndAmendmentCode(Long id, String amendmentCode);
}
