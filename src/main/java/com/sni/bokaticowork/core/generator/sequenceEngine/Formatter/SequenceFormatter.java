package com.sni.bokaticowork.core.generator.sequenceEngine.Formatter;

import com.sni.bokaticowork.core.generator.sequenceEngine.exceptions.InvalidSequenceConfigurationException;
import com.sni.bokaticowork.core.generator.sequenceEngine.model.SequenceDefinition;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class SequenceFormatter {

    public String format(SequenceDefinition definition, LocalDate date, Long value){

        validate(definition);

        String pattern = definition.getPattern();
        String seqFormatted = leftPad(value, definition.getPadding());

        String prefix = safe(definition.getPrefix());
        String suffix = safe(definition.getSuffix());

        return pattern
                .replace("{PREFIX}", prefix)
                .replace("{SUFFIX}", suffix)
                .replace("{YYYY}", String.valueOf(date.getYear()))
                .replace("{YY}", String.format("%02d", date.getYear() % 100))
                .replace("{MM}", String.format("%02d", date.getMonthValue()))
                .replace("{DD}", String.format("%02d", date.getDayOfMonth()))
                .replace("{SEQ}", seqFormatted);


    }

    private void validate(SequenceDefinition definition) {
        if (definition == null) {
            throw new InvalidSequenceConfigurationException("Sequence definition must not be null");
        }
        if (definition.getPattern() == null || definition.getPattern().isBlank()) {
            throw new InvalidSequenceConfigurationException("Sequence pattern must not be blank");
        }
        if (definition.getPadding() == null || definition.getPadding() < 1) {
            throw new InvalidSequenceConfigurationException("Sequence padding must be greater than zero");
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String leftPad(long value, int padding) {
        return String.format("%0" + padding + "d", value);
    }
}


