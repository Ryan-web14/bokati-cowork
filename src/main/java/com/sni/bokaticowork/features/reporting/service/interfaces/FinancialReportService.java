package com.sni.bokaticowork.features.reporting.service.interfaces;

import com.sni.bokaticowork.features.reporting.dto.response.BillingAgingReportResponse;
import com.sni.bokaticowork.features.reporting.dto.response.CashFlowReportResponse;
import com.sni.bokaticowork.features.reporting.dto.response.CashRegisterReportResponse;
import com.sni.bokaticowork.features.reporting.dto.response.FinancialDashboardResponse;
import com.sni.bokaticowork.features.reporting.dto.response.PaymentSourceReportResponse;

import java.time.LocalDate;

public interface FinancialReportService {

    FinancialDashboardResponse dashboard(LocalDate from, LocalDate to);

    PaymentSourceReportResponse paymentSources(LocalDate from, LocalDate to);

    BillingAgingReportResponse billingAging();

    CashRegisterReportResponse cashRegisters(LocalDate from, LocalDate to);

    CashFlowReportResponse cashFlow(LocalDate from, LocalDate to);
}