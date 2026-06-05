package com.sni.bokaticowork.features.analytics.service;

import com.sni.bokaticowork.features.analytics.dto.AnalyticsOverviewResponse;
import com.sni.bokaticowork.features.analytics.dto.BookingTrendResponse;
import com.sni.bokaticowork.features.analytics.dto.TopOwnerResponse;
import com.sni.bokaticowork.features.analytics.dto.TopResourceResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface AnalyticsService {

    AnalyticsOverviewResponse overview(LocalDate fromDate, LocalDate toDate);

    AnalyticsOverviewResponse.FinancialMetrics financial(Instant fromDate, Instant toDate);

    AnalyticsOverviewResponse.BookingMetrics booking(Instant fromDate, Instant toDate);

    List<BookingTrendResponse> bookingTrend(Instant fromDate, Instant toDate, String groupBy);

    List<TopOwnerResponse> topOwners(Instant fromDate, Instant toDate, int limit);

    List<TopResourceResponse> topResources(Instant fromDate, Instant toDate, int limit);
}
