package com.sni.bokaticowork.features.reporting.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.reporting.dto.response.DebtRecoveryReportResponse;
import com.sni.bokaticowork.features.reporting.service.interfaces.DebtRecoveryReportService;
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

import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@RequestMapping({ApiPath.V1 + "/reports/debt-recovery", ApiPath.V1 + "/reporting/debt-recovery"})
public class DebtRecoveryReportController {

    private final DebtRecoveryReportService service;
    private final ReportPdfService reportPdfService;

    @GetMapping
    public ResponseEntity<DebtRecoveryReportResponse> debtRecovery(
            @RequestParam(defaultValue = "age") String sortBy) {
        return ResponseEntity.ok(service.debtRecovery(sortBy));
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> debtRecoveryPdf(
            @RequestParam(defaultValue = "age") String sortBy) {
        byte[] pdf = reportPdfService.debtRecoveryReportPdf(sortBy);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename("rapport-recouvrement.pdf").build());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    @GetMapping("/csv")
    public ResponseEntity<byte[]> debtRecoveryCsv(
            @RequestParam(defaultValue = "age") String sortBy) {
        byte[] csv = service.debtRecoveryCsv(sortBy);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment().filename("rapport-recouvrement.csv").build());
        return ResponseEntity.ok().headers(headers).body(csv);
    }
}
