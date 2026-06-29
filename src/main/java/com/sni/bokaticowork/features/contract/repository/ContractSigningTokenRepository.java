package com.sni.bokaticowork.features.contract.repository;

import com.sni.bokaticowork.features.contract.model.ContractSigningToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContractSigningTokenRepository extends JpaRepository<ContractSigningToken, Long> {

    Optional<ContractSigningToken> findByToken(UUID token);

    Optional<ContractSigningToken> findByContractCodeAndRevokedFalseAndSignedAtIsNull(String contractCode);
}
