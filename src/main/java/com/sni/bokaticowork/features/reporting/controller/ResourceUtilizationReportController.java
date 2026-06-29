package com.sni.bokaticowork.features.reporting.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.reporting.dto.response.ResourceUtilizationReportResponse;
import com.sni.bokaticowork.features.reporting.service.interfaces.ReportPdfService;
import com.sni.bokaticowork.features.reporting.service.interfaces.ResourceUtilizationReportService;
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
@RequestMapping({ApiPath.V1 + "/reports/resource-utilization", ApiPath.V1 + "/reporting/resource-utilization"})
public class ResourceUtilizationReportController {

    private final ResourceUtilizationReportService service;
    private final ReportPdfService reportPdfService;

    @GetMapping
    public ResponseEntity<ResourceUtilizationReportResponse> resourceUtilization(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        return ResponseEntity.ok(service.resourceUtilization(
                resolveDate(fromDate, from), resolveDate(toDate, to)));
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> resourceUtilizationPdf(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        byte[] pdf = reportPdfService.resourceUtilizationReportPdf(
                resolveDate(fromDate, from), resolveDate(toDate, to));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename("rapport-utilisation-ressources.pdf").build());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    private LocalDate resolveDate(String preferred, String fallback) {
        return parseDate(preferred == null || preferred.isBlank() ? fallback : preferred);
    }

    private LocalDate parseDate(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value.trim());
    }
}
