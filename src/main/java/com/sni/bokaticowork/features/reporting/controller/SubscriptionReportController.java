package com.sni.bokaticowork.features.reporting.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.reporting.dto.response.SubscriptionReportResponse;
import com.sni.bokaticowork.features.reporting.service.interfaces.ReportPdfService;
import com.sni.bokaticowork.features.reporting.service.interfaces.SubscriptionReportService;
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
@RequestMapping({ApiPath.V1 + "/reports/subscriptions", ApiPath.V1 + "/reporting/subscriptions"})
public class SubscriptionReportController {

    private final SubscriptionReportService service;
    private final ReportPdfService reportPdfService;

    @GetMapping
    public ResponseEntity<SubscriptionReportResponse> subscriptionReport(
            @RequestParam(defaultValue = "12") int months,
            @RequestParam(defaultValue = "30") int renewalDays) {
        return ResponseEntity.ok(service.subscriptionReport(months, renewalDays));
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> subscriptionReportPdf(
            @RequestParam(defaultValue = "12") int months,
            @RequestParam(defaultValue = "30") int renewalDays) {
        byte[] pdf = reportPdfService.subscriptionReportPdf(months, renewalDays);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename("rapport-abonnements.pdf").build());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
