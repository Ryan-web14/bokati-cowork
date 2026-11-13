package com.sni.bokaticowork.features.accounting.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.accounting.service.implementation.ChequeService;
import com.sni.bokaticowork.features.accounting.service.implementation.StatementPrintService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Pieces imprimables ajoutees au lot D4 des modeles de documents : bordereau de remise en banque,
 * releves fournisseur et partenaire. Droits : ceux des routes de lecture voisines
 * ({@code AdminApiAuthorizationManager}, comptabilite).
 */
@RestController
@RequestMapping(ApiPath.V1 + "/accounting")
@RequiredArgsConstructor
public class AccountingPrintController {

    private final ChequeService cheques;
    private final StatementPrintService statements;

    @GetMapping(value = "/deposit-slips/{number}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> depositSlip(@PathVariable String number) {
        return pdf(cheques.slipPdf(number), "bordereau-" + number);
    }

    @GetMapping(value = "/purchases/suppliers/{code}/statement/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> supplierStatement(@PathVariable String code,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return pdf(statements.supplierStatement(code, from, to), "releve-fournisseur-" + code);
    }

    @GetMapping(value = "/partners/{code}/statement/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> partnerStatement(@PathVariable String code,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return pdf(statements.partnerStatement(code, from, to), "releve-partenaire-" + code);
    }

    private static ResponseEntity<byte[]> pdf(byte[] body, String name) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + name.replaceAll("[^A-Za-z0-9._-]", "_") + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(body);
    }
}
