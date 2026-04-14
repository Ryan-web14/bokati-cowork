package com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces;

import com.sni.bokaticowork.core.generator.sequenceEngine.model.SequenceCounter;

public interface    SequenceCounterService {

    long nextSequence(String periodKey, Long defintionId, String sequenceCode, long initialValue, int incrementStep);
    SequenceCounter getSequenceCounterForService(Long defintionId, String periodKey);

}
