package com.sni.bokaticowork.features.reporting.service.interfaces;

import com.sni.bokaticowork.features.reporting.dto.response.ActivityReportResponse;

import java.time.LocalDate;

public interface ActivityReportService {

    ActivityReportResponse activityReport(LocalDate from, LocalDate to, boolean compareWithPrevious);

    byte[] activityReportCsv(LocalDate from, LocalDate to, boolean compareWithPrevious);
}
