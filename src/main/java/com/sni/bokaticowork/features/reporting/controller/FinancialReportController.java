package com.sni.bokaticowork.features.reporting.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.reporting.dto.response.BillingAgingReportResponse;
import com.sni.bokaticowork.features.reporting.dto.response.CashFlowReportResponse;
import com.sni.bokaticowork.features.reporting.dto.response.CashRegisterReportResponse;
import com.sni.bokaticowork.features.reporting.dto.response.FinancialDashboardResponse;
import com.sni.bokaticowork.features.reporting.dto.response.PaymentSourceReportResponse;
import com.sni.bokaticowork.features.reporting.service.interfaces.FinancialReportService;
import com.sni.bokaticowork.features.reporting.service.interfaces.ReportPdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/reports/finance")
public class FinancialReportController {

    private final FinancialReportService service;
    private final ReportPdfService reportPdfService;

    /**
     * GET /reports/finance/dashboard?from=2026-04-01&to=2026-04-30
     * Global financial KPIs: revenue, collection rate, payment breakdown, discounts, VAT.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<FinancialDashboardResponse> dashboard(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(service.dashboard(parseDate(from), parseDate(to)));
    }

    /**
     * GET /reports/finance/payments?from=2026-04-01&to=2026-04-30
     * Payment method breakdown + daily series (cash, wallet, mobile money, …).
     */
    @GetMapping("/payments")
    public ResponseEntity<PaymentSourceReportResponse> payments(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(service.paymentSources(parseDate(from), parseDate(to)));
    }

    /**
     * GET /reports/finance/aging
     * Outstanding invoice aging buckets (current, 1-30d, 31-60d, 61-90d, 90+d).
     */
    @GetMapping("/aging")
    public ResponseEntity<BillingAgingReportResponse> aging() {
        return ResponseEntity.ok(service.billingAging());
    }

    /**
     * GET /reports/finance/cash-registers?from=2026-04-01&to=2026-04-30
     * Per-register summary: cash in/out, session count, variance.
     */
    @GetMapping("/cash-registers")
    public ResponseEntity<CashRegisterReportResponse> cashRegisters(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(service.cashRegisters(parseDate(from), parseDate(to)));
    }

    /**
     * GET /reports/finance/cash-flow?from=2026-04-01&to=2026-04-30
     * Daily cash flow statement: inflows vs outflows by movement type.
     */
    @GetMapping("/cash-flow")
    public ResponseEntity<CashFlowReportResponse> cashFlow(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(service.cashFlow(parseDate(from), parseDate(to)));
    }

    @GetMapping("/dashboard/pdf")
    public ResponseEntity<byte[]> dashboardPdf(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        byte[] pdf = reportPdfService.financialDashboardPdf(parseDate(from), parseDate(to));
        return pdfResponse(pdf, "rapport-financier.pdf");
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    private LocalDate parseDate(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value.trim());
    }
}