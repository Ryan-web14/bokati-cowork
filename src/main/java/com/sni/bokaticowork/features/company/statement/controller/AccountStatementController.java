package com.sni.bokaticowork.features.company.statement.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.company.statement.dto.StatementDtos;
import com.sni.bokaticowork.features.company.statement.service.AccountStatementExporter;
import com.sni.bokaticowork.features.company.statement.service.AccountStatementService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Les etats de compte cote administration · d'une organisation, d'un membre ; en JSON, CSV, PDF.
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
public class AccountStatementController {

    private final AccountStatementService service;
    private final AccountStatementExporter exporter;

    @GetMapping(ApiPath.V1 + "/companies/{customerId}/statement")
    public ResponseEntity<StatementDtos.Statement> company(@PathVariable String customerId,
                                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(service.company(customerId, from, to));
    }

    @GetMapping(ApiPath.V1 + "/companies/{customerId}/statement.pdf")
    public ResponseEntity<byte[]> companyPdf(@PathVariable String customerId,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return pdf(service.company(customerId, from, to));
    }

    @GetMapping(ApiPath.V1 + "/companies/{customerId}/statement.csv")
    public ResponseEntity<String> companyCsv(@PathVariable String customerId,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return csv(service.company(customerId, from, to));
    }

    @GetMapping(ApiPath.V1 + "/members/{memberId}/statement")
    public ResponseEntity<StatementDtos.Statement> member(@PathVariable String memberId,
                                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(service.member(memberId, from, to));
    }

    @GetMapping(ApiPath.V1 + "/members/{memberId}/statement.pdf")
    public ResponseEntity<byte[]> memberPdf(@PathVariable String memberId,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return pdf(service.member(memberId, from, to));
    }

    @GetMapping(ApiPath.V1 + "/members/{memberId}/statement.csv")
    public ResponseEntity<String> memberCsv(@PathVariable String memberId,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return csv(service.member(memberId, from, to));
    }

    private ResponseEntity<byte[]> pdf(StatementDtos.Statement statement) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + AccountStatementExporter.fileName(statement, "pdf") + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(exporter.pdf(statement));
    }

    private ResponseEntity<String> csv(StatementDtos.Statement statement) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + AccountStatementExporter.fileName(statement, "csv") + "\"")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(exporter.csv(statement));
    }
}
