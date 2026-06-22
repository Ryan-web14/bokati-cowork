package com.sni.bokaticowork.features.reporting.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.reporting.dto.response.ActivityReportResponse;
import com.sni.bokaticowork.features.reporting.service.interfaces.ActivityReportService;
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
@RequestMapping({ApiPath.V1 + "/reports/activity", ApiPath.V1 + "/reporting/activity"})
public class ActivityReportController {

    private final ActivityReportService service;
    private final ReportPdfService reportPdfService;

    @GetMapping
    public ResponseEntity<ActivityReportResponse> activityReport(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "false") boolean compareWithPrevious) {
        return ResponseEntity.ok(service.activityReport(
                resolveDate(fromDate, from), resolveDate(toDate, to), compareWithPrevious));
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> activityReportPdf(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "false") boolean compareWithPrevious) {
        byte[] pdf = reportPdfService.activityReportPdf(
                resolveDate(fromDate, from), resolveDate(toDate, to), compareWithPrevious);
        return pdfResponse(pdf, "rapport-activite.pdf");
    }

    @GetMapping("/csv")
    public ResponseEntity<byte[]> activityReportCsv(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "false") boolean compareWithPrevious) {
        byte[] csv = service.activityReportCsv(
                resolveDate(fromDate, from), resolveDate(toDate, to), compareWithPrevious);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment().filename("rapport-activite.csv").build());
        return ResponseEntity.ok().headers(headers).body(csv);
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    private LocalDate resolveDate(String preferred, String fallback) {
        return parseDate(preferred == null || preferred.isBlank() ? fallback : preferred);
    }

    private LocalDate parseDate(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value.trim());
    }
}
