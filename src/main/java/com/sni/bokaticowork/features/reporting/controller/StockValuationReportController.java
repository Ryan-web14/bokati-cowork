package com.sni.bokaticowork.features.reporting.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.reporting.dto.response.StockValuationReportResponse;
import com.sni.bokaticowork.features.reporting.service.interfaces.ReportPdfService;
import com.sni.bokaticowork.features.reporting.service.interfaces.StockValuationReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping({ApiPath.V1 + "/reports/stock-valuation", ApiPath.V1 + "/reporting/stock-valuation"})
public class StockValuationReportController {

    private final StockValuationReportService service;
    private final ReportPdfService reportPdfService;

    @GetMapping
    public ResponseEntity<StockValuationReportResponse> stockValuation(
            @RequestParam(defaultValue = "30") int inactiveDays) {
        return ResponseEntity.ok(service.stockValuation(inactiveDays));
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> stockValuationPdf(
            @RequestParam(defaultValue = "30") int inactiveDays) {
        byte[] pdf = reportPdfService.stockValuationReportPdf(inactiveDays);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename("rapport-valorisation-stock.pdf").build());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
