package com.sni.bokaticowork.features.reporting.service.interfaces;

import com.sni.bokaticowork.features.reporting.dto.response.StockValuationReportResponse;

public interface StockValuationReportService {

    StockValuationReportResponse stockValuation(int inactiveDays);
}
