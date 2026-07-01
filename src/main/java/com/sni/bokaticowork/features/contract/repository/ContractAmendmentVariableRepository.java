package com.sni.bokaticowork.features.contract.repository;

import com.sni.bokaticowork.features.contract.model.ContractAmendmentVariable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContractAmendmentVariableRepository extends JpaRepository<ContractAmendmentVariable, Long> {
    List<ContractAmendmentVariable> findAllByAmendmentCodeOrderByVariableKeyAsc(String amendmentCode);
    Optional<ContractAmendmentVariable> findByAmendmentCodeAndVariableKey(String amendmentCode, String variableKey);
    void deleteAllByAmendmentCode(String amendmentCode);
}
