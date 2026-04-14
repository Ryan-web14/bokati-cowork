package com.sni.bokaticowork.core.generator.sequenceEngine.exceptions;

public class SequenceDisabledException extends RuntimeException {
    public SequenceDisabledException(String code) {
        super(
                String.format("Sequence with code %s is disabled", code));
    }
}
