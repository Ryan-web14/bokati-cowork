package com.sni.bokaticowork.core.generator.sequenceEngine.service.impl;

import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@RequiredArgsConstructor
@Service
public class SequenceGeneratorFacadeImpl implements SequenceGeneratorFacade {

    private final SequenceService sequenceService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String next(String code) {
        return sequenceService.nextSequence(code, LocalDate.now());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String next(String code, LocalDate date) {
        return sequenceService.nextSequence(code, date);
    }
}
