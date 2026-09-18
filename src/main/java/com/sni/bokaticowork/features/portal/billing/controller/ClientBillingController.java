package com.sni.bokaticowork.features.portal.billing.controller;

import com.sni.bokaticowork.features.portal.wallet.dto.ClientWalletPinRequest;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.payment.dto.response.MobileMoneyDepositResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentIntentResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentReceiptResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentTransactionResponse;
import com.sni.bokaticowork.features.portal.billing.dto.request.ClientInitiateMobileMoneyPaymentRequest;
import com.sni.bokaticowork.features.portal.billing.dto.response.ClientInvoiceResponse;
import com.sni.bokaticowork.features.portal.billing.dto.response.ClientInvoiceSummaryResponse;
import com.sni.bokaticowork.features.portal.billing.dto.response.ClientSpendingSummaryResponse;
import com.sni.bokaticowork.features.portal.billing.service.ClientBillingService;
import com.sni.bokaticowork.features.portal.context.ClientContextService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping(ApiPath.V1 + "/client/billing")
@RequiredArgsConstructor
public class ClientBillingController {

    private final ClientContextService clientContextService;
    private final ClientBillingService clientBillingService;

    // ── Invoices ─────────────────────────────────────────────────────────────

    @GetMapping("/invoices")
    public ResponseEntity<PaginatedResponse<ClientInvoiceSummaryResponse>> listInvoices(
            @RequestParam(required = false) BillingDocumentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "issueDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientBillingService.listInvoices(member, status, from, to, pageable));
    }

    @GetMapping("/invoices/payable")
    public ResponseEntity<PaginatedResponse<ClientInvoiceSummaryResponse>> listPayableInvoices(
            @PageableDefault(size = 20, sort = "dueDate", direction = Sort.Direction.ASC) Pageable pageable) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientBillingService.listPayableInvoices(member, pageable));
    }

    @GetMapping("/invoices/{documentNumber}")
    public ResponseEntity<ClientInvoiceResponse> getInvoice(@PathVariable String documentNumber) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientBillingService.getInvoice(member, documentNumber));
    }

    @GetMapping("/invoices/{documentNumber}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable String documentNumber) {
        Member member = clientContextService.getAuthenticatedMember();
        byte[] pdf = clientBillingService.downloadInvoicePdf(member, documentNumber);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"invoice-" + documentNumber + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ── Payments ─────────────────────────────────────────────────────────────

    @PostMapping("/invoices/{documentNumber}/pay/mobile-money")
    public ResponseEntity<MobileMoneyDepositResponse> payWithMobileMoney(
            @PathVariable String documentNumber,
            @Valid @RequestBody ClientInitiateMobileMoneyPaymentRequest request) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientBillingService.payWithMobileMoney(member, documentNumber, request));
    }

    @PostMapping("/invoices/{documentNumber}/pay/wallet")
    public ResponseEntity<PaymentTransactionResponse> payWithWallet(
            @PathVariable String documentNumber,
            @RequestBody(required = false) ClientWalletPinRequest body) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientBillingService.payWithWallet(member, documentNumber,
                body == null ? null : body.pin()));
    }

    @GetMapping("/payments")
    public ResponseEntity<PaginatedResponse<PaymentIntentResponse>> listPayments(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientBillingService.listPayments(member, pageable));
    }

    @GetMapping("/payments/{transactionNumber}/receipt")
    public ResponseEntity<PaymentReceiptResponse> getReceipt(@PathVariable String transactionNumber) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientBillingService.getReceipt(member, transactionNumber));
    }

    @GetMapping("/payments/{transactionNumber}/receipt/pdf")
    public ResponseEntity<byte[]> downloadReceiptPdf(@PathVariable String transactionNumber) {
        Member member = clientContextService.getAuthenticatedMember();
        byte[] pdf = clientBillingService.downloadReceiptPdf(member, transactionNumber);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"receipt-" + transactionNumber + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ── Summary ──────────────────────────────────────────────────────────────

    @GetMapping("/summary")
    public ResponseEntity<ClientSpendingSummaryResponse> getSpendingSummary() {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientBillingService.getSpendingSummary(member));
    }
}
