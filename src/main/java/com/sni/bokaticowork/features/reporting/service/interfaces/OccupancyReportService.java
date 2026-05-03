package com.sni.bokaticowork.features.reporting.service.interfaces;

import com.sni.bokaticowork.features.reporting.dto.response.OccupancyReportResponse;

import java.time.LocalDate;

public interface OccupancyReportService {

    OccupancyReportResponse occupancy(LocalDate from, LocalDate to);
}