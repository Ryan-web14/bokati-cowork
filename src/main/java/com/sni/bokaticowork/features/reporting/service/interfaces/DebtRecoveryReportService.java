package com.sni.bokaticowork.features.reporting.service.interfaces;

import com.sni.bokaticowork.features.reporting.dto.response.DebtRecoveryReportResponse;

public interface DebtRecoveryReportService {

    DebtRecoveryReportResponse debtRecovery(String sortBy);

    byte[] debtRecoveryCsv(String sortBy);
}
