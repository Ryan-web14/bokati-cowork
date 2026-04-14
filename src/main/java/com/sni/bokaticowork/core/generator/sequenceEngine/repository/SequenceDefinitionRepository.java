package com.sni.bokaticowork.core.generator.sequenceEngine.repository;

import com.sni.bokaticowork.core.generator.sequenceEngine.model.SequenceDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SequenceDefinitionRepository extends JpaRepository<SequenceDefinition, Long> {
    Optional<SequenceDefinition> findByCodeIgnoreCase(String code);
}
