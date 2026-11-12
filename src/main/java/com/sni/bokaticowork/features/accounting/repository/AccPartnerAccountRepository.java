package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccPartnerAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccPartnerAccountRepository extends JpaRepository<AccPartnerAccount, Long> {

    java.util.Optional<AccPartnerAccount> findByCode(String code);

    java.util.List<AccPartnerAccount> findAllByOrderByCodeAsc();
}
