package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccVatReturn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccVatReturnRepository extends JpaRepository<AccVatReturn, Long> {

    Optional<AccVatReturn> findByPeriodCode(String periodCode);

    List<AccVatReturn> findTop36ByOrderByPeriodCodeDesc();
}
