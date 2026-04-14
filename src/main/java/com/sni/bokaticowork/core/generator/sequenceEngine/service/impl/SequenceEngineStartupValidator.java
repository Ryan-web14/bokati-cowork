package com.sni.bokaticowork.core.generator.sequenceEngine.service.impl;

import com.sni.bokaticowork.core.generator.sequenceEngine.exceptions.InvalidSequenceConfigurationException;
import com.sni.bokaticowork.core.generator.sequenceEngine.model.SequenceDefinition;
import com.sni.bokaticowork.core.generator.sequenceEngine.repository.SequenceDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SequenceEngineStartupValidator implements ApplicationRunner {

    private final SequenceDefinitionRepository sequenceDefinitionRepository;

    @Override
    public void run(ApplicationArguments args) {
        validateDefinitions(sequenceDefinitionRepository.findAll());
    }

    void validateDefinitions(List<SequenceDefinition> definitions) {
        Set<String> normalizedCodes = new HashSet<>();

        for (SequenceDefinition definition : definitions) {
            validateDefinition(definition);

            String normalizedCode = definition.getCode().trim().toLowerCase(Locale.ROOT);
            if (!normalizedCodes.add(normalizedCode)) {
                throw new InvalidSequenceConfigurationException(
                        "Duplicate sequence definition code found during startup: " + normalizedCode
                );
            }
        }
    }

    private void validateDefinition(SequenceDefinition definition) {
        if (definition == null) {
            throw new InvalidSequenceConfigurationException("Sequence definition must not be null");
        }
        if (definition.getCode() == null || definition.getCode().isBlank()) {
            throw new InvalidSequenceConfigurationException("Sequence definition code must not be blank");
        }
        if (definition.getPattern() == null || definition.getPattern().isBlank()) {
            throw new InvalidSequenceConfigurationException(
                    "Sequence pattern must not be blank for code: " + definition.getCode()
            );
        }
        if (!definition.getPattern().contains("{SEQ}")) {
            throw new InvalidSequenceConfigurationException(
                    "Sequence pattern must contain {SEQ} for code: " + definition.getCode()
            );
        }
        if (definition.getResetPolicy() == null) {
            throw new InvalidSequenceConfigurationException(
                    "Reset policy must not be null for code: " + definition.getCode()
            );
        }
        if (definition.getInitialValue() == null || definition.getInitialValue() < 1) {
            throw new InvalidSequenceConfigurationException(
                    "Initial value must be greater than zero for code: " + definition.getCode()
            );
        }
        if (definition.getIncrementStep() == null || definition.getIncrementStep() < 1) {
            throw new InvalidSequenceConfigurationException(
                    "Increment step must be greater than zero for code: " + definition.getCode()
            );
        }
        if (definition.getPadding() == null || definition.getPadding() < 1) {
            throw new InvalidSequenceConfigurationException(
                    "Padding must be greater than zero for code: " + definition.getCode()
            );
        }
    }
}
