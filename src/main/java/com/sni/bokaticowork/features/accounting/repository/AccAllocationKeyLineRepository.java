package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccAllocationKeyLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccAllocationKeyLineRepository extends JpaRepository<AccAllocationKeyLine, Long> {

    java.util.List<AccAllocationKeyLine> findByKeyId(Long keyId);
}
