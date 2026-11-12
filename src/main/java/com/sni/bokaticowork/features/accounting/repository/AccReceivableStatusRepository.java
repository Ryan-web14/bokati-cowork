package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccReceivableStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AccReceivableStatusRepository extends JpaRepository<AccReceivableStatus, Long> {
    Optional<AccReceivableStatus> findByDocumentNumber(String documentNumber);
    List<AccReceivableStatus> findAllByCustomerCode(String customerCode);
    List<AccReceivableStatus> findAllByStatusIn(java.util.Collection<String> statuses);
}
