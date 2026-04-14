package com.sni.bokaticowork.core.generator.sequenceEngine.service.impl;

import com.sni.bokaticowork.core.generator.sequenceEngine.Formatter.SequenceFormatter;
import com.sni.bokaticowork.core.generator.sequenceEngine.enums.ResetPolicy;
import com.sni.bokaticowork.core.generator.sequenceEngine.exceptions.InvalidSequenceConfigurationException;
import com.sni.bokaticowork.core.generator.sequenceEngine.model.SequenceDefinition;
import com.sni.bokaticowork.core.generator.sequenceEngine.repository.SequenceDefinitionRepository;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceCounterService;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceResetPolicyResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SequenceServiceImplTest {

    @Mock
    private SequenceDefinitionRepository definitionRepository;

    @Mock
    private SequenceResetPolicyResolver policyResolver;

    @Mock
    private SequenceFormatter sequenceFormatter;

    @Mock
    private SequenceCounterService counterService;

    @InjectMocks
    private SequenceServiceImpl sequenceService;

    @Test
    void shouldUseConfiguredInitialValueAndIncrementStep() {
        SequenceDefinition definition = buildDefinition();
        LocalDate date = LocalDate.of(2026, 3, 31);

        when(definitionRepository.findByCodeIgnoreCase("MEMBER")).thenReturn(Optional.of(definition));
        when(policyResolver.resolveResetPolicy(ResetPolicy.NEVER, date)).thenReturn("GLOBAL");
        when(counterService.nextSequence("GLOBAL", 99L, "member", 10L, 5)).thenReturn(10L);
        when(sequenceFormatter.format(definition, date, 10L)).thenReturn("MBR-000010");

        String result = sequenceService.nextSequence(" MEMBER ", date);

        assertEquals("MBR-000010", result);
        verify(counterService).nextSequence("GLOBAL", 99L, "member", 10L, 5);
    }

    @Test
    void shouldRejectPatternWithoutSeqToken() {
        SequenceDefinition definition = buildDefinition();
        definition.setPattern("{PREFIX}-{YYYY}");

        when(definitionRepository.findByCodeIgnoreCase("member")).thenReturn(Optional.of(definition));

        assertThrows(InvalidSequenceConfigurationException.class,
                () -> sequenceService.nextSequence("member", LocalDate.now()));
    }

    private SequenceDefinition buildDefinition() {
        SequenceDefinition definition = new SequenceDefinition();
        definition.setId(99L);
        definition.setCode("member");
        definition.setPattern("{PREFIX}-{SEQ}");
        definition.setPrefix("MBR");
        definition.setPadding(6);
        definition.setInitialValue(10L);
        definition.setIncrementStep(5);
        definition.setResetPolicy(ResetPolicy.NEVER);
        definition.setEnabled(true);
        return definition;
    }
}
