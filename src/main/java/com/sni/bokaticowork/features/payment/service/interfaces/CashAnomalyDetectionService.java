package com.sni.bokaticowork.features.payment.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.payment.dto.request.ReviewCashAnomalyRequest;
import com.sni.bokaticowork.features.payment.dto.response.CashAnomalyFlagResponse;
import com.sni.bokaticowork.features.payment.enums.CashAnomalySeverity;
import com.sni.bokaticowork.features.payment.enums.CashAnomalyStatus;
import com.sni.bokaticowork.features.payment.enums.CashAnomalyType;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CashAnomalyDetectionService {

    List<CashAnomalyFlagResponse> analyzeSession(String sessionNumber);

    PaginatedResponse<CashAnomalyFlagResponse> list(String registerCode, String sessionNumber, CashAnomalySeverity severity,
                                                     CashAnomalyStatus status, CashAnomalyType anomalyType, Pageable pageable);

    CashAnomalyFlagResponse review(String flagNumber, ReviewCashAnomalyRequest request);
}
