package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccReceivableWriteoff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AccReceivableWriteoffRepository extends JpaRepository<AccReceivableWriteoff, Long> {
    List<AccReceivableWriteoff> findAllByOrderByCreatedAtDesc();
    Optional<AccReceivableWriteoff> findByVoucherNumber(String voucherNumber);
}
