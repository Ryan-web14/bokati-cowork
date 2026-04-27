package com.sni.bokaticowork.features.analytics.service;

import com.sni.bokaticowork.features.analytics.dto.AnalyticsOverviewResponse;

import java.time.Instant;
import java.time.LocalDate;

public interface AnalyticsService {

    AnalyticsOverviewResponse overview(LocalDate fromDate, LocalDate toDate);

    AnalyticsOverviewResponse.FinancialMetrics financial(Instant fromDate, Instant toDate);

    AnalyticsOverviewResponse.BookingMetrics booking(Instant fromDate, Instant toDate);
}
