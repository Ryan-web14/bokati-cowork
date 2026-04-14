package com.sni.bokaticowork.features.contract.repository;

import com.sni.bokaticowork.features.contract.enums.ContractStatus;
import com.sni.bokaticowork.features.contract.model.Contract;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract, Long>, JpaSpecificationExecutor<Contract> {
    Optional<Contract> findByContractCode(String contractCode);
    boolean existsByContractCode(String contractCode);
    Page<Contract> findAllByOwnerTypeAndOwnerCode(DocumentOwnerType ownerType, String ownerCode, Pageable pageable);
    List<Contract> findAllByStatus(ContractStatus status);
    List<Contract> findAllByStatusAndDeletedFalse(ContractStatus status);
    List<Contract> findAllByStatusInAndEndDateBeforeAndDeletedFalse(List<ContractStatus> statuses, LocalDate date);
}
