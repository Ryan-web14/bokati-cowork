package com.sni.bokaticowork.core.generator.sequenceEngine.service.impl;

import com.sni.bokaticowork.core.generator.sequenceEngine.enums.ResetPolicy;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SequenceResetPolicyResolverImplTest {

    private final SequenceResetPolicyResolverImpl resolver = new SequenceResetPolicyResolverImpl();

    @Test
    void shouldReturnGlobalKeyForNeverPolicy() {
        String key = resolver.resolveResetPolicy(ResetPolicy.NEVER, LocalDate.of(2026, 3, 31));

        assertEquals("GLOBAL", key);
    }

    @Test
    void shouldReturnMonthlyKey() {
        String key = resolver.resolveResetPolicy(ResetPolicy.MONTHLY, LocalDate.of(2026, 3, 31));

        assertEquals("202603", key);
    }

    @Test
    void shouldRejectNullInputs() {
        assertThrows(IllegalArgumentException.class, () -> resolver.resolveResetPolicy(null, LocalDate.now()));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolveResetPolicy(ResetPolicy.DAILY, null));
    }
}
