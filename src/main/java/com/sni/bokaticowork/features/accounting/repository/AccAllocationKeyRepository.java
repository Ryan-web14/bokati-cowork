package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccAllocationKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccAllocationKeyRepository extends JpaRepository<AccAllocationKey, Long> {

    java.util.List<AccAllocationKey> findByCodeOrderByEffectiveFromDesc(String code);

    java.util.List<AccAllocationKey> findAllByOrderByCodeAscEffectiveFromDesc();
}
