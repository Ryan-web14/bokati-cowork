package com.sni.bokaticowork.features.reporting.service.interfaces;

import com.sni.bokaticowork.features.reporting.dto.response.ResourceUtilizationReportResponse;

import java.time.LocalDate;

public interface ResourceUtilizationReportService {

    ResourceUtilizationReportResponse resourceUtilization(LocalDate from, LocalDate to);
}
