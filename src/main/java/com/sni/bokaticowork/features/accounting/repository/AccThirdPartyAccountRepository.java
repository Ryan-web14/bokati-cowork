package com.sni.bokaticowork.features.accounting.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AccThirdPartyAccountRepository extends JpaRepository<com.sni.bokaticowork.features.accounting.model.AccThirdPartyAccount, Long> {

    Optional<com.sni.bokaticowork.features.accounting.model.AccThirdPartyAccount> findByAccountNumberAndPartyCode(String accountNumber, String partyCode);

    List<com.sni.bokaticowork.features.accounting.model.AccThirdPartyAccount> findByAccountNumberOrderByPartyCodeAsc(String accountNumber);

    Optional<com.sni.bokaticowork.features.accounting.model.AccThirdPartyAccount> findFirstByPartyCodeOrderByIdAsc(String partyCode);
}
