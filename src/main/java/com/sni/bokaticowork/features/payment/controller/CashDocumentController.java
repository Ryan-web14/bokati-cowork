package com.sni.bokaticowork.features.payment.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.payment.service.interfaces.CashDocumentPdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPath.V1 + "/cash-registers")
@RequiredArgsConstructor
public class CashDocumentController {

    private final CashDocumentPdfService cashDocumentPdfService;

    @GetMapping("/movements/{movementNumber}/document")
    public ResponseEntity<byte[]> movementDocument(@PathVariable String movementNumber) {
        return pdfResponse(movementNumber, cashDocumentPdfService.generateMovementDocument(movementNumber));
    }

    @GetMapping("/requests/{requestNumber}/document")
    public ResponseEntity<byte[]> requestDocument(@PathVariable String requestNumber) {
        return pdfResponse(requestNumber, cashDocumentPdfService.generateRequestDocument(requestNumber));
    }

    @GetMapping("/sessions/{sessionNumber}/closing-report")
    public ResponseEntity<byte[]> sessionClosingReport(@PathVariable String sessionNumber) {
        return pdfResponse(sessionNumber, cashDocumentPdfService.generateSessionClosingReport(sessionNumber));
    }

    private ResponseEntity<byte[]> pdfResponse(String fileNameBase, byte[] pdf) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileNameBase + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
