package com.sni.bokaticowork.core.generator.sequenceEngine.service.impl;

import com.sni.bokaticowork.core.generator.sequenceEngine.enums.ResetPolicy;
import com.sni.bokaticowork.core.generator.sequenceEngine.exceptions.InvalidSequenceConfigurationException;
import com.sni.bokaticowork.core.generator.sequenceEngine.model.SequenceDefinition;
import com.sni.bokaticowork.core.generator.sequenceEngine.repository.SequenceDefinitionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class SequenceEngineStartupValidatorTest {

    @Mock
    private SequenceDefinitionRepository sequenceDefinitionRepository;

    @InjectMocks
    private SequenceEngineStartupValidator validator;

    @Test
    void shouldRejectDuplicateCodesIgnoringCase() {
        SequenceDefinition first = buildDefinition("member");
        SequenceDefinition second = buildDefinition("MEMBER");

        assertThrows(
                InvalidSequenceConfigurationException.class,
                () -> validator.validateDefinitions(List.of(first, second))
        );
    }

    @Test
    void shouldAcceptValidDefinitions() {
        assertDoesNotThrow(() -> validator.validateDefinitions(List.of(buildDefinition("member"))));
    }

    private SequenceDefinition buildDefinition(String code) {
        SequenceDefinition definition = new SequenceDefinition();
        definition.setCode(code);
        definition.setPattern("{PREFIX}-{SEQ}");
        definition.setPadding(6);
        definition.setInitialValue(1L);
        definition.setIncrementStep(1);
        definition.setResetPolicy(ResetPolicy.NEVER);
        return definition;
    }
}
