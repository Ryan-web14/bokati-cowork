package com.sni.bokaticowork.core.baseClasses.repository;

import com.sni.bokaticowork.core.baseClasses.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    Address findByStreetNumber(String streetNumber);
    List<Address> findByStreetName(String streetName);
}
