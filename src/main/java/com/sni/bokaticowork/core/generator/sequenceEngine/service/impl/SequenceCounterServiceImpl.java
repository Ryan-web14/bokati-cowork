package com.sni.bokaticowork.core.generator.sequenceEngine.service.impl;


import com.sni.bokaticowork.core.generator.sequenceEngine.model.SequenceCounter;
import com.sni.bokaticowork.core.generator.sequenceEngine.repository.SequenceCounterRepository;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceCounterService;
import com.sni.bokaticowork.core.generator.id.GeneratorOfId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Transactional
@Service
@RequiredArgsConstructor
public class SequenceCounterServiceImpl implements SequenceCounterService {

    private static final GeneratorOfId ID_GENERATOR = new GeneratorOfId();

    private final SequenceCounterRepository sequenceCounterRepo;

    @Override
    public long nextSequence(String periodKey, Long defintionId, String sequenceCode, long initialValue, int incrementStep) {
        if (periodKey == null || periodKey.isBlank()) {
            throw new IllegalArgumentException("Period key must not be blank");
        }
        if (defintionId == null) {
            throw new IllegalArgumentException("Sequence definition id must not be null");
        }
        if (sequenceCode == null || sequenceCode.isBlank()) {
            throw new IllegalArgumentException("Sequence code must not be blank");
        }
        if (initialValue < 1) {
            throw new IllegalArgumentException("Initial value must be greater than zero");
        }
        if (incrementStep < 1) {
            throw new IllegalArgumentException("Increment step must be greater than zero");
        }

        String normalizedSequenceCode = sequenceCode.trim().toLowerCase();

        Optional<SequenceCounter> sequenceCounter = sequenceCounterRepo.findByPeriodKeyAndSequenceDefinitionId(periodKey, defintionId);

        if(sequenceCounter.isPresent()){
            return advanceCounter(sequenceCounter.get(), normalizedSequenceCode, incrementStep);
        }

        long nextValueToStore = initialValue + incrementStep;
        int inserted = sequenceCounterRepo.insertIfAbsent(
                ID_GENERATOR.generateId(),
                defintionId,
                normalizedSequenceCode,
                periodKey,
                nextValueToStore
        );

        if (inserted > 0) {
            return initialValue;
        }

        SequenceCounter concurrentCounter = sequenceCounterRepo.findByPeriodKeyAndSequenceDefinitionId(periodKey, defintionId)
                .orElseThrow(() -> new RuntimeException("Conflict on creating the sequence for " + normalizedSequenceCode));
        return advanceCounter(concurrentCounter, normalizedSequenceCode, incrementStep);
    }

    private long advanceCounter(SequenceCounter counter, String sequenceCode, int incrementStep) {
        if (counter.getSequenceCode() == null || counter.getSequenceCode().isBlank()) {
            counter.setSequenceCode(sequenceCode);
        }
        long current = counter.getCurrentValue();
        counter.setCurrentValue(current + incrementStep);
        counter.setUpdatedAt(Instant.now());
        sequenceCounterRepo.save(counter);
        return current;
    }

    @Override
    public SequenceCounter getSequenceCounterForService(Long defintionId, String periodKey) {
        return sequenceCounterRepo.findByPeriodKeyAndSequenceDefinitionId(periodKey, defintionId)
                .orElseThrow(() -> new RuntimeException("No sequence counter found for " + periodKey + " and " + defintionId));
    }

}
