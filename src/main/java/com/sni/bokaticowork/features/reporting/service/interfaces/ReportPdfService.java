package com.sni.bokaticowork.features.reporting.service.interfaces;

import java.time.LocalDate;

public interface ReportPdfService {
    byte[] financialDashboardPdf(LocalDate from, LocalDate to);
    byte[] occupancyPdf(LocalDate from, LocalDate to);
    byte[] activityReportPdf(LocalDate from, LocalDate to, boolean compareWithPrevious);
    byte[] memberProfileReportPdf(String memberId);
    byte[] debtRecoveryReportPdf(String sortBy);
    byte[] resourceUtilizationReportPdf(LocalDate from, LocalDate to);
    byte[] stockValuationReportPdf(int inactiveDays);
    byte[] subscriptionReportPdf(int months, int renewalDays);
}