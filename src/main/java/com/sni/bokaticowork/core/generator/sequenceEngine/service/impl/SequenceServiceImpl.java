package com.sni.bokaticowork.core.generator.sequenceEngine.service.impl;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.Formatter.SequenceFormatter;
import com.sni.bokaticowork.core.generator.sequenceEngine.exceptions.InvalidSequenceConfigurationException;
import com.sni.bokaticowork.core.generator.sequenceEngine.exceptions.SequenceDisabledException;
import com.sni.bokaticowork.core.generator.sequenceEngine.model.SequenceDefinition;
import com.sni.bokaticowork.core.generator.sequenceEngine.repository.SequenceDefinitionRepository;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceCounterService;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceResetPolicyResolver;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@RequiredArgsConstructor
@Service
@Transactional
@Slf4j
public class SequenceServiceImpl implements SequenceService {

    private final SequenceDefinitionRepository defintionRepository;
    private final SequenceResetPolicyResolver policyResolver;
    private final SequenceFormatter sequenceFormatter;
    private final SequenceCounterService counterService;


    @Override
    public String nextSequence(String code, LocalDate date) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Sequence code must not be blank");
        }
        if (date == null) {
            throw new IllegalArgumentException("Business date must not be null");
        }

        SequenceDefinition definition = defintionRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Sequence definition not found for code: " + code));

        if (Boolean.FALSE.equals(definition.getEnabled())) {
            throw new SequenceDisabledException(definition.getCode());
        }

        validateDefinition(definition);

        String policy = policyResolver.resolveResetPolicy(definition.getResetPolicy(), date);
        Long value = counterService.nextSequence(
                policy,
                definition.getId(),
                definition.getCode().trim().toLowerCase(),
                definition.getInitialValue(),
                definition.getIncrementStep()
        );

        return sequenceFormatter.format(definition, date, value);
    }

    private void validateDefinition(SequenceDefinition definition) {
        if (definition.getInitialValue() == null || definition.getInitialValue() < 1) {
            throw new InvalidSequenceConfigurationException(
                    "Initial value must be greater than zero for sequence code: " + definition.getCode()
            );
        }

        if (definition.getIncrementStep() == null || definition.getIncrementStep() < 1) {
            throw new InvalidSequenceConfigurationException(
                    "Increment step must be greater than zero for sequence code: " + definition.getCode()
            );
        }

        if (definition.getResetPolicy() == null) {
            throw new InvalidSequenceConfigurationException(
                    "Reset policy must not be null for sequence code: " + definition.getCode()
            );
        }
        if (definition.getCode() == null || definition.getCode().isBlank()) {
            throw new InvalidSequenceConfigurationException("Sequence code must not be blank");
        }
        if (definition.getPattern() == null || definition.getPattern().isBlank()) {
            throw new InvalidSequenceConfigurationException(
                    "Sequence pattern must not be blank for sequence code: " + definition.getCode()
            );
        }
        if (!definition.getPattern().contains("{SEQ}")) {
            throw new InvalidSequenceConfigurationException(
                    "Sequence pattern must contain {SEQ} for sequence code: " + definition.getCode()
            );
        }
    }
}
