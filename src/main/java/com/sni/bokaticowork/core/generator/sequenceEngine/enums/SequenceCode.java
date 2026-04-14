package com.sni.bokaticowork.core.generator.sequenceEngine.enums;

public enum SequenceCode {
    ENTRY("seq.entry.pattern"),
    INVOICE("seq.invoice.pattern"),
    VENDOR("seq.vendor.pattern");

    private final String patternKey;
    SequenceCode(String patternKey) { this.patternKey = patternKey; }
    public String patternKey() { return patternKey; }
}