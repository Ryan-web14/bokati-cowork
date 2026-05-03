package com.sni.bokaticowork.features.payment.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.payment.dto.response.PaymentReceiptResponse;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPath.V1 + "/payments")
@RequiredArgsConstructor
public class PaymentReceiptController {

    private final PaymentReceiptService paymentReceiptService;

    @GetMapping("/transactions/{transactionNumber}/receipt")
    public ResponseEntity<PaymentReceiptResponse> getByTransaction(@PathVariable String transactionNumber) {
        return ResponseEntity.ok(paymentReceiptService.getByTransactionNumber(transactionNumber));
    }

    @GetMapping("/transactions/{transactionNumber}/receipt/pdf")
    public ResponseEntity<byte[]> pdfByTransaction(@PathVariable String transactionNumber) {
        PaymentReceiptResponse receipt = paymentReceiptService.getByTransactionNumber(transactionNumber);
        byte[] pdf = paymentReceiptService.generatePdfByTransactionNumber(transactionNumber);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + receipt.receiptNumber() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/receipts/{receiptNumber}")
    public ResponseEntity<PaymentReceiptResponse> getByReceipt(@PathVariable String receiptNumber) {
        return ResponseEntity.ok(paymentReceiptService.getByReceiptNumber(receiptNumber));
    }

    @GetMapping("/receipts/{receiptNumber}/pdf")
    public ResponseEntity<byte[]> pdfByReceipt(@PathVariable String receiptNumber) {
        byte[] pdf = paymentReceiptService.generatePdfByReceiptNumber(receiptNumber);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + receiptNumber + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
