package com.sni.bokaticowork.features.contract.repository;

import com.sni.bokaticowork.features.contract.enums.AmendmentStatus;
import com.sni.bokaticowork.features.contract.model.ContractAmendment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContractAmendmentRepository extends JpaRepository<ContractAmendment, Long> {
    Optional<ContractAmendment> findByCode(String code);
    List<ContractAmendment> findAllByOriginalContractCodeOrderByCreatedAtDesc(String originalContractCode);
    List<ContractAmendment> findAllByOriginalContractCodeAndStatusIn(String originalContractCode, List<AmendmentStatus> statuses);
}
