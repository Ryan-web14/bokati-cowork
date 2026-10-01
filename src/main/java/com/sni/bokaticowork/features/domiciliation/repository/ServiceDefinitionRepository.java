package com.sni.bokaticowork.features.domiciliation.repository;

import com.sni.bokaticowork.features.domiciliation.model.ServiceDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceDefinitionRepository extends JpaRepository<ServiceDefinition, Long> {

    Optional<ServiceDefinition> findByCode(String code);

    List<ServiceDefinition> findByActiveTrueOrderByNameAsc();

    List<ServiceDefinition> findAllByOrderByNameAsc();
}
