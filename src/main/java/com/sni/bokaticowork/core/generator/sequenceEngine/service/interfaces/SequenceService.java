package com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces;

import java.time.LocalDate;

public interface SequenceService {

    String nextSequence(String code, LocalDate date);

}
