package com.sni.bokaticowork.features.payment.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.dto.request.CreateReconciliationBatchRequest;
import com.sni.bokaticowork.features.payment.dto.response.ReconciliationBatchResponse;
import com.sni.bokaticowork.features.payment.enums.ReconciliationStatus;
import com.sni.bokaticowork.features.payment.mapper.interfaces.PaymentMapper;
import com.sni.bokaticowork.features.payment.model.PaymentReconciliationBatch;
import com.sni.bokaticowork.features.payment.repository.PaymentReconciliationBatchRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Service
@Transactional
@RequiredArgsConstructor
public class PaymentReconciliationServiceImpl implements PaymentReconciliationService {

    private final PaymentReconciliationBatchRepository batchRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final PaymentMapper mapper;

    @Override
    public ReconciliationBatchResponse create(CreateReconciliationBatchRequest request) {
        PaymentReconciliationBatch batch = batchRepository.save(PaymentReconciliationBatch.builder()
                .batchNumber(sequenceGenerator.next("payment_reconciliation_batch"))
                .provider(request.provider().trim().toUpperCase())
                .status(ReconciliationStatus.OPEN)
                .createdBy(trim(request.createdBy()))
                .build());
        return mapper.toReconciliationBatchResponse(batch);
    }

    @Override
    public ReconciliationBatchResponse complete(String batchNumber) {
        PaymentReconciliationBatch batch = batchRepository.findByBatchNumber(batchNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Reconciliation batch not found"));
        if (batch.getStatus() != ReconciliationStatus.OPEN) {
            throw new BadRequestException("Reconciliation batch is not open");
        }
        batch.setStatus(ReconciliationStatus.COMPLETED);
        batch.setCompletedAt(Instant.now());
        return mapper.toReconciliationBatchResponse(batchRepository.save(batch));
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
