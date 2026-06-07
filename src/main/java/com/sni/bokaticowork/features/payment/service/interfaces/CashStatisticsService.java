package com.sni.bokaticowork.features.payment.service.interfaces;

import com.sni.bokaticowork.features.payment.dto.response.CashRegisterStatisticsResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashSessionStatisticsResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashierStatisticsResponse;

import java.time.Instant;

public interface CashStatisticsService {

    CashSessionStatisticsResponse sessionStatistics(String sessionNumber);

    CashierStatisticsResponse cashierStatistics(String cashierCode);

    CashRegisterStatisticsResponse registerStatistics(String registerCode, Instant fromDate, Instant toDate);
}
