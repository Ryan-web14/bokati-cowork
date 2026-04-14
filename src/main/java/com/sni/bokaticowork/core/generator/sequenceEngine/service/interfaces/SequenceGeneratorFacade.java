package com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces;

import java.time.LocalDate;

public interface SequenceGeneratorFacade {

    String next(String code);
    String next(String code, LocalDate date);
}
