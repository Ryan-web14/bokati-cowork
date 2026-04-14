package com.sni.bokaticowork.features.contract.repository;

import com.sni.bokaticowork.features.contract.model.Contract;
import com.sni.bokaticowork.features.contract.model.ContractParty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractPartyRepository extends JpaRepository<ContractParty, Long> {
    List<ContractParty> findAllByContractOrderBySignOrderAscIdAsc(Contract contract);
}
