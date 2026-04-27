package com.sni.bokaticowork.features.payment.service.interfaces;

import com.sni.bokaticowork.features.payment.dto.request.CreateReconciliationBatchRequest;
import com.sni.bokaticowork.features.payment.dto.response.ReconciliationBatchResponse;

public interface PaymentReconciliationService {
    ReconciliationBatchResponse create(CreateReconciliationBatchRequest request);
    ReconciliationBatchResponse complete(String batchNumber);
}
