package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccSupplier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccSupplierRepository extends JpaRepository<AccSupplier, Long> {

    java.util.Optional<AccSupplier> findByCode(String code);

    java.util.List<AccSupplier> findAllByOrderByNameAsc();
}
