package com.sni.bokaticowork.features.domiciliation.repository;

import com.sni.bokaticowork.features.domiciliation.model.DomiciliationAddress;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DomiciliationAddressRepository extends JpaRepository<DomiciliationAddress, Long> {

    /** L'adresse postale et son pays sont lus avec l'adresse de domiciliation : AddressView les lit hors transaction. */
    @EntityGraph(attributePaths = {"address", "address.country"})
    Optional<DomiciliationAddress> findByCode(String code);

    @EntityGraph(attributePaths = {"address", "address.country"})
    List<DomiciliationAddress> findAllByOrderByLabelAsc();
}
