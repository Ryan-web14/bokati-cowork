package com.sni.bokaticowork.core.generator.sequenceEngine.service.impl;

import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@RequiredArgsConstructor
@Service
@Transactional
public class SequenceGeneratorFacadeImpl implements SequenceGeneratorFacade {

    private final SequenceService sequenceService;

    @Override
    public String next(String code) {
        return sequenceService.nextSequence(code, LocalDate.now());
    }

    @Override
    public String next(String code, LocalDate date) {
        return sequenceService.nextSequence(code, date);
    }
}
