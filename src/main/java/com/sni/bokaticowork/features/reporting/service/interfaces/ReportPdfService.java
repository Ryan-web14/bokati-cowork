package com.sni.bokaticowork.features.reporting.service.interfaces;

import java.time.LocalDate;

public interface ReportPdfService {
    byte[] financialDashboardPdf(LocalDate from, LocalDate to);
    byte[] occupancyPdf(LocalDate from, LocalDate to);
}