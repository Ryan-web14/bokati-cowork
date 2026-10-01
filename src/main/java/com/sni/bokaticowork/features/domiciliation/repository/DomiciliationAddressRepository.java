package com.sni.bokaticowork.features.domiciliation.repository;

import com.sni.bokaticowork.features.domiciliation.model.DomiciliationAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DomiciliationAddressRepository extends JpaRepository<DomiciliationAddress, Long> {

    Optional<DomiciliationAddress> findByCode(String code);

    List<DomiciliationAddress> findAllByOrderByLabelAsc();
}
