package com.sni.bokaticowork.features.reporting.service.interfaces;

import com.sni.bokaticowork.features.reporting.dto.response.SubscriptionReportResponse;

public interface SubscriptionReportService {

    SubscriptionReportResponse subscriptionReport(int months, int renewalDays);
}
