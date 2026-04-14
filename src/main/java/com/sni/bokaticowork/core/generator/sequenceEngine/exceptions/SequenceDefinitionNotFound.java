package com.sni.bokaticowork.core.generator.sequenceEngine.exceptions;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;

public class SequenceDefinitionNotFound extends ResourceNotFoundException {
    public SequenceDefinitionNotFound(String code) {
        super(
                String.format("Sequence definition with code %s not found", code)
        );
    }
}
