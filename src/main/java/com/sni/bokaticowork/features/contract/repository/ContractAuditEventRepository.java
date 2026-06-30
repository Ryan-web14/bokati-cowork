package com.sni.bokaticowork.features.contract.repository;

import com.sni.bokaticowork.features.contract.model.ContractAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContractAuditEventRepository extends JpaRepository<ContractAuditEvent, Long> {
    List<ContractAuditEvent> findAllByContractCodeOrderByOccurredAtAsc(String contractCode);
    Optional<ContractAuditEvent> findTopByContractCodeOrderByOccurredAtDesc(String contractCode);
}
