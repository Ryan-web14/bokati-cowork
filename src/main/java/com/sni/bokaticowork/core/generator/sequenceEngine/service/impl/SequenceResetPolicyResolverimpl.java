package com.sni.bokaticowork.core.generator.sequenceEngine.service.impl;

import com.sni.bokaticowork.core.generator.sequenceEngine.enums.ResetPolicy;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceResetPolicyResolver;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class SequenceResetPolicyResolverImpl implements SequenceResetPolicyResolver {

    private static final String GLOBAL_PERIOD_KEY = "GLOBAL";

    @Override
    public String resolveResetPolicy(ResetPolicy resetPolicy, LocalDate date) {

        if(resetPolicy == null){
            throw new IllegalArgumentException("Reset policy must not be null");
        }

        if(date == null){
            throw new IllegalArgumentException("Business date must not be null");
        }

        return switch (resetPolicy) {
            case NEVER -> GLOBAL_PERIOD_KEY;
            case YEARLY -> String.valueOf(date.getYear());
            case MONTHLY -> "%04d%02d".formatted(date.getYear(), date.getMonthValue());
            case DAILY -> "%04d%02d%02d".formatted(date.getYear(),
                    date.getMonthValue(), date.getDayOfMonth());
        };

    }
}
