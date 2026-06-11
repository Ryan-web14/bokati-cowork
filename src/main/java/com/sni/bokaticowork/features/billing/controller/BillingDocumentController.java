package com.sni.bokaticowork.features.billing.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateCreditNoteRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateInvoiceFromBillableItemsRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateManualBillingDocumentRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateReservationInvoiceRequest;
import com.sni.bokaticowork.features.billing.dto.request.RequestSignatureRequest;
import com.sni.bokaticowork.features.billing.dto.request.SelectQuoteOptionsRequest;
import com.sni.bokaticowork.features.billing.dto.request.UpdateBillingDocumentRequest;
import com.sni.bokaticowork.features.billing.dto.response.BillingAgingReportResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentSignatureResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingRecoverableResponse;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingRecoverableService;
import com.sni.bokaticowork.features.billing.service.interfaces.QuoteSignatureService;
import com.sni.bokaticowork.features.billing.dto.request.AddRecoverableItemsRequest;
import com.sni.bokaticowork.features.billing.dto.request.RecoverItemRequest;
import com.sni.bokaticowork.features.billing.dto.request.WriteOffRecoverableRequest;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.dto.response.CustomerStatementResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentPdfService;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingEmailService;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.payment.dto.request.
        PayInvoiceRequest;
import com.sni.bokaticowork.features.payment.dto.response.PayInvoiceResponse;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping(ApiPath.V1 + "/billing")
@RequiredArgsConstructor
public class BillingDocumentController {

    private final BillingDocumentService billingDocumentService;
    private final BillingDocumentPdfService billingDocumentPdfService;
    private final BillingEmailService billingEmailService;
    private final PaymentService paymentService;
    private final QuoteSignatureService quoteSignatureService;
    private final BillingRecoverableService billingRecoverableService;

    @PostMapping("/documents")
    public ResponseEntity<BillingDocumentResponse> create(@Valid @RequestBody CreateBillingDocumentRequest request) {
        return ResponseEntity.ok(billingDocumentService.create(request));
    }

    @PostMapping("/invoices/manual")
    public ResponseEntity<BillingDocumentResponse> createManualInvoice(@Valid @RequestBody CreateManualBillingDocumentRequest request) {
        return ResponseEntity.ok(billingDocumentService.createManualInvoice(request));
    }

    @PostMapping("/quotes/manual")
    public ResponseEntity<BillingDocumentResponse> createManualQuote(@Valid @RequestBody CreateManualBillingDocumentRequest request) {
        return ResponseEntity.ok(billingDocumentService.createManualQuote(request));
    }

    @PostMapping("/quotations/manual")
    public ResponseEntity<BillingDocumentResponse> createManualQuotation(@Valid @RequestBody CreateManualBillingDocumentRequest request) {
        return ResponseEntity.ok(billingDocumentService.createManualQuotation(request));
    }

    @PostMapping("/invoices/from-billable-items")
    public ResponseEntity<BillingDocumentResponse> createInvoiceFromBillableItems(@Valid @RequestBody CreateInvoiceFromBillableItemsRequest request) {
        return ResponseEntity.ok(billingDocumentService.createInvoiceFromBillableItems(request));
    }

    @PostMapping("/invoices/from-reservation")
    public ResponseEntity<BillingDocumentResponse> createInvoiceFromReservation(@Valid @RequestBody CreateReservationInvoiceRequest request) {
        return ResponseEntity.ok(billingDocumentService.createInvoiceFromReservation(request));
    }

    @PutMapping("/documents/{documentNumber}")
    public ResponseEntity<BillingDocumentResponse> update(@PathVariable String documentNumber,
                                                          @Valid @RequestBody UpdateBillingDocumentRequest request) {
        return ResponseEntity.ok(billingDocumentService.update(documentNumber, request));
    }

    @PostMapping("/documents/{documentNumber}/duplicate")
    public ResponseEntity<BillingDocumentResponse> duplicate(@PathVariable String documentNumber) {
        return ResponseEntity.ok(billingDocumentService.duplicate(documentNumber));
    }

    @PatchMapping("/documents/{documentNumber}/issue")
    public ResponseEntity<BillingDocumentResponse> issue(@PathVariable String documentNumber) {
        return ResponseEntity.ok(billingDocumentService.issue(documentNumber));
    }

    @PatchMapping("/documents/{documentNumber}/send")
    public ResponseEntity<BillingDocumentResponse> send(@PathVariable String documentNumber) {
        return ResponseEntity.ok(billingDocumentService.send(documentNumber));
    }

    @PatchMapping("/quotes/{quoteNumber}/mark-viewed")
    public ResponseEntity<BillingDocumentResponse> markViewed(@PathVariable String quoteNumber) {
        return ResponseEntity.ok(billingDocumentService.markViewed(quoteNumber));
    }

    @PatchMapping("/quotes/{quoteNumber}/start-negotiation")
    public ResponseEntity<BillingDocumentResponse> startNegotiation(@PathVariable String quoteNumber) {
        return ResponseEntity.ok(billingDocumentService.startNegotiation(quoteNumber));
    }

    @PatchMapping("/quotes/{quoteNumber}/request-deposit")
    public ResponseEntity<BillingDocumentResponse> requestDeposit(@PathVariable String quoteNumber) {
        return ResponseEntity.ok(billingDocumentService.requestDeposit(quoteNumber));
    }

    @PatchMapping("/quotes/{quoteNumber}/mark-deposit-paid")
    public ResponseEntity<BillingDocumentResponse> markDepositPaid(@PathVariable String quoteNumber) {
        return ResponseEntity.ok(billingDocumentService.markDepositPaid(quoteNumber));
    }

    @PostMapping("/quotes/{quoteNumber}/request-signature")
    public ResponseEntity<BillingDocumentSignatureResponse> requestSignature(
            @PathVariable String quoteNumber,
            @RequestBody(required = false) RequestSignatureRequest request) {
        return ResponseEntity.ok(quoteSignatureService.requestSignature(quoteNumber, request));
    }

    @PatchMapping("/quotes/{quoteNumber}/select-options")
    public ResponseEntity<BillingDocumentResponse> selectQuoteOptions(@PathVariable String quoteNumber,
                                                                      @Valid @RequestBody SelectQuoteOptionsRequest request) {
        return ResponseEntity.ok(billingDocumentService.selectQuoteOptions(quoteNumber, request));
    }

    @PatchMapping("/quotes/{quoteNumber}/accept")
    public ResponseEntity<BillingDocumentResponse> acceptQuote(@PathVariable String quoteNumber) {
        return ResponseEntity.ok(billingDocumentService.acceptQuote(quoteNumber));
    }

    @PatchMapping("/quotations/{quotationNumber}/accept")
    public ResponseEntity<BillingDocumentResponse> acceptQuotation(@PathVariable String quotationNumber) {
        return ResponseEntity.ok(billingDocumentService.acceptQuotation(quotationNumber));
    }

    @PatchMapping("/quotes/{quoteNumber}/reject")
    public ResponseEntity<BillingDocumentResponse> rejectQuote(@PathVariable String quoteNumber) {
        return ResponseEntity.ok(billingDocumentService.rejectQuote(quoteNumber));
    }

    @PatchMapping("/quotations/{quotationNumber}/reject")
    public ResponseEntity<BillingDocumentResponse> rejectQuotation(@PathVariable String quotationNumber) {
        return ResponseEntity.ok(billingDocumentService.rejectQuotation(quotationNumber));
    }

    @PostMapping("/quotes/{quoteNumber}/convert-to-invoice")
    public ResponseEntity<BillingDocumentResponse> convertQuoteToInvoice(@PathVariable String quoteNumber) {
        return ResponseEntity.ok(billingDocumentService.convertQuoteToInvoice(quoteNumber));
    }

    @PostMapping("/quotations/{quotationNumber}/convert-to-invoice")
    public ResponseEntity<BillingDocumentResponse> convertQuotationToInvoice(@PathVariable String quotationNumber) {
        return ResponseEntity.ok(billingDocumentService.convertQuotationToInvoice(quotationNumber));
    }

    @PostMapping("/invoices/{invoiceNumber}/credit-note")
    public ResponseEntity<BillingDocumentResponse> createCreditNote(@PathVariable String invoiceNumber,
                                                                    @Valid @RequestBody CreateCreditNoteRequest request) {
        return ResponseEntity.ok(billingDocumentService.createCreditNote(invoiceNumber, request));
    }

    @PatchMapping("/credit-notes/{creditNoteNumber}/apply")
    public ResponseEntity<BillingDocumentResponse> applyCreditNote(@PathVariable String creditNoteNumber) {
        return ResponseEntity.ok(billingDocumentService.applyCreditNote(creditNoteNumber));
    }

    @PostMapping("/invoices/{documentNumber}/pay")
    public ResponseEntity<PayInvoiceResponse> payInvoice(@PathVariable String documentNumber,
                                                         @Valid @RequestBody PayInvoiceRequest request) {
        return ResponseEntity.ok(paymentService.payInvoice(documentNumber, request));
    }

    @GetMapping("/documents/{documentNumber}")
    public ResponseEntity<BillingDocumentResponse> get(@PathVariable String documentNumber) {
        return ResponseEntity.ok(billingDocumentService.get(documentNumber));
    }

    @GetMapping("/documents/{documentNumber}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable String documentNumber) {
        byte[] pdf = billingDocumentPdfService.generatePdf(documentNumber);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + documentNumber + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/documents/{documentNumber}/send-email")
    public ResponseEntity<Boolean> sendEmail(@PathVariable String documentNumber) {
        return ResponseEntity.ok(billingEmailService.sendDocument(documentNumber));
    }

    @GetMapping("/customers/{customerType}/{customerCode}/statement")
    public ResponseEntity<CustomerStatementResponse> statement(
            @PathVariable String customerType,
            @PathVariable String customerCode,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(billingDocumentService.customerStatement(customerType, customerCode, pageable));
    }

    @GetMapping("/customers/{customerType}/{customerCode}/statement.csv")
    public ResponseEntity<String> statementCsv(
            @PathVariable String customerType,
            @PathVariable String customerCode,
            @PageableDefault(size = 500, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        CustomerStatementResponse statement = billingDocumentService.customerStatement(customerType, customerCode, pageable);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"statement-" + customerType + "-" + customerCode + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(toStatementCsv(statement));
    }

    @GetMapping("/documents")
    public ResponseEntity<PaginatedResponse<BillingDocumentResponse>> list(
            @RequestParam(required = false) BillingDocumentType type,
            @RequestParam(required = false) BillingDocumentStatus status,
            @RequestParam(required = false) String customerType,
            @RequestParam(required = false) String customerCode,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String sourceCode,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) String searchText,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(billingDocumentService.list(type, status, customerType, customerCode, sourceType, sourceCode, fromDate, toDate, searchText, pageable));
    }

    @PostMapping("/documents/{documentNumber}/recoverables")
    public ResponseEntity<java.util.List<BillingRecoverableResponse>> addRecoverables(
            @PathVariable String documentNumber,
            @Valid @RequestBody AddRecoverableItemsRequest request) {
        return ResponseEntity.ok(billingRecoverableService.addItems(documentNumber, request));
    }

    @GetMapping("/documents/{documentNumber}/recoverables")
    public ResponseEntity<java.util.List<BillingRecoverableResponse>> listRecoverables(
            @PathVariable String documentNumber,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(billingRecoverableService.list(documentNumber, status));
    }

    @PatchMapping("/documents/{documentNumber}/recoverables/{recoverableNumber}/recover")
    public ResponseEntity<BillingRecoverableResponse> recover(
            @PathVariable String documentNumber,
            @PathVariable String recoverableNumber,
            @RequestBody(required = false) RecoverItemRequest request) {
        return ResponseEntity.ok(billingRecoverableService.markRecovered(documentNumber, recoverableNumber, request));
    }

    @PatchMapping("/documents/{documentNumber}/recoverables/{recoverableNumber}/write-off")
    public ResponseEntity<BillingRecoverableResponse> writeOff(
            @PathVariable String documentNumber,
            @PathVariable String recoverableNumber,
            @RequestBody(required = false) WriteOffRecoverableRequest request) {
        return ResponseEntity.ok(billingRecoverableService.writeOff(documentNumber, recoverableNumber, request));
    }

    @GetMapping("/aging-report")
    public ResponseEntity<BillingAgingReportResponse> agingReport(
            @RequestParam(required = false) String customerType,
            @RequestParam(required = false) String customerCode,
            @RequestParam(required = false, defaultValue = "XAF") String currency) {
        return ResponseEntity.ok(billingRecoverableService.agingReport(customerType, customerCode, currency));
    }

    private String toStatementCsv(CustomerStatementResponse statement) {
        StringBuilder csv = new StringBuilder("documentNumber,type,status,issueDate,dueDate,totalAmount,paidAmount,balanceDue,currency\n");
        for (BillingDocumentResponse document : statement.documents()) {
            csv.append(csv(document.documentNumber())).append(',')
                    .append(document.documentType()).append(',')
                    .append(document.status()).append(',')
                    .append(document.issueDate()).append(',')
                    .append(document.dueDate()).append(',')
                    .append(document.totalAmount()).append(',')
                    .append(document.paidAmount()).append(',')
                    .append(document.balanceDue()).append(',')
                    .append(csv(document.currency())).append('\n');
        }
        return csv.toString();
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        return value.contains(",") || value.contains("\"") || value.contains("\n")
                ? "\"" + value.replace("\"", "\"\"") + "\""
                : value;
    }
}
