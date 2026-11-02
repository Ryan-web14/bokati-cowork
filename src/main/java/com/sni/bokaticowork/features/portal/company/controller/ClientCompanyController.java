package com.sni.bokaticowork.features.portal.company.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.company.membership.dto.MembershipDtos;
import com.sni.bokaticowork.features.company.membership.service.CompanyMembershipService;
import com.sni.bokaticowork.features.company.statement.dto.StatementDtos;
import com.sni.bokaticowork.features.company.statement.service.AccountStatementExporter;
import com.sni.bokaticowork.features.payment.cash.dto.request.DeclareCashPaymentRequest;
import com.sni.bokaticowork.features.payment.cash.dto.response.CashPaymentDeclarationResponse;
import com.sni.bokaticowork.features.payment.dto.response.MobileMoneyDepositResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentReceiptResponse;
import com.sni.bokaticowork.features.portal.billing.dto.request.ClientInitiateMobileMoneyPaymentRequest;
import com.sni.bokaticowork.features.portal.billing.dto.response.ClientInvoiceResponse;
import com.sni.bokaticowork.features.portal.company.service.ClientCompanyBillingService;
import com.sni.bokaticowork.features.portal.context.ClientContextService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Les organisations du membre connecte · et, pour le gerant, ce qu'il pilote.
 *
 * <p>Un employe voit ses organisations et son role ; seul le gerant voit les membres et regle le
 * mode de rattachement des factures. Une organisation dont le membre n'est pas gerant repond 404,
 * sans dire qu'elle existe.</p>
 *
 * <p>Le gerant y trouve aussi le compte de l'organisation : etat de compte, factures de
 * l'organisation et de ses membres qui lui sont reliees, et leur reglement.</p>
 */
@RestController
@RequestMapping(ApiPath.V1 + "/client/companies")
@RequiredArgsConstructor
public class ClientCompanyController {

    private final ClientContextService clientContextService;
    private final CompanyMembershipService service;
    private final ClientCompanyBillingService billingService;
    private final AccountStatementExporter statementExporter;

    @GetMapping
    public ResponseEntity<List<MembershipDtos.MyCompany>> myCompanies() {
        return ResponseEntity.ok(service.myCompanies(clientContextService.getAuthenticatedMember()));
    }

    @GetMapping("/{customerId}/members")
    public ResponseEntity<List<MembershipDtos.LinkResponse>> members(@PathVariable String customerId) {
        return ResponseEntity.ok(service.membersForOwner(clientContextService.getAuthenticatedMember(), customerId));
    }

    @PatchMapping("/{customerId}/invoice-link-mode")
    @Audited(module = "COMPANY", action = "INVOICE_LINK_MODE_PORTAL", ressource = "customer")
    public ResponseEntity<MembershipDtos.MyCompany> setDefaultMode(@PathVariable String customerId,
                                                                   @Valid @RequestBody MembershipDtos.CompanyModeRequest request) {
        return ResponseEntity.ok(service.setCompanyDefaultMode(clientContextService.getAuthenticatedMember(), customerId, request.mode()));
    }

    @PatchMapping("/{customerId}/members/{linkId}/invoice-link-mode")
    @Audited(module = "COMPANY", action = "INVOICE_LINK_MODE_PORTAL", ressource = "customer_member_link")
    public ResponseEntity<MembershipDtos.LinkResponse> setLinkMode(@PathVariable String customerId, @PathVariable Long linkId,
                                                                   @Valid @RequestBody MembershipDtos.LinkModeRequest request) {
        return ResponseEntity.ok(service.setLinkMode(clientContextService.getAuthenticatedMember(), customerId, linkId, request));
    }

    // ── Compte de l'organisation (gerant) ────────────────────────────────────

    @GetMapping("/{customerId}/statement")
    public ResponseEntity<StatementDtos.Statement> statement(@PathVariable String customerId,
                                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(billingService.statement(clientContextService.getAuthenticatedMember(), customerId, from, to));
    }

    @GetMapping("/{customerId}/statement.pdf")
    public ResponseEntity<byte[]> statementPdf(@PathVariable String customerId,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        StatementDtos.Statement statement = billingService.statement(clientContextService.getAuthenticatedMember(), customerId, from, to);
        return pdf(AccountStatementExporter.fileName(statement, "pdf"), statementExporter.pdf(statement));
    }

    @GetMapping("/{customerId}/invoices/{documentNumber}")
    public ResponseEntity<ClientInvoiceResponse> invoice(@PathVariable String customerId, @PathVariable String documentNumber) {
        return ResponseEntity.ok(billingService.invoice(clientContextService.getAuthenticatedMember(), customerId, documentNumber));
    }

    @GetMapping("/{customerId}/invoices/{documentNumber}/pdf")
    public ResponseEntity<byte[]> invoicePdf(@PathVariable String customerId, @PathVariable String documentNumber) {
        return pdf("invoice-" + documentNumber + ".pdf",
                billingService.invoicePdf(clientContextService.getAuthenticatedMember(), customerId, documentNumber));
    }

    @PostMapping("/{customerId}/invoices/{documentNumber}/pay/mobile-money")
    @Audited(module = "COMPANY", action = "INVOICE_PAY_PORTAL", ressource = "billing_document")
    public ResponseEntity<MobileMoneyDepositResponse> payWithMobileMoney(@PathVariable String customerId,
                                                                         @PathVariable String documentNumber,
                                                                         @Valid @RequestBody ClientInitiateMobileMoneyPaymentRequest request) {
        return ResponseEntity.ok(billingService.payWithMobileMoney(clientContextService.getAuthenticatedMember(),
                customerId, documentNumber, request));
    }

    @PostMapping("/{customerId}/invoices/{documentNumber}/pay/cash")
    @Audited(module = "COMPANY", action = "INVOICE_CASH_DECLARATION_PORTAL", ressource = "billing_document")
    public ResponseEntity<CashPaymentDeclarationResponse> declareCashPayment(@PathVariable String customerId,
                                                                             @PathVariable String documentNumber,
                                                                             @Valid @RequestBody(required = false) DeclareCashPaymentRequest request) {
        return ResponseEntity.ok(billingService.declareCashPayment(clientContextService.getAuthenticatedMember(),
                customerId, documentNumber, request));
    }

    @GetMapping("/{customerId}/mobile-money/deposits/{depositId}")
    public ResponseEntity<MobileMoneyDepositResponse> mobileMoneyDeposit(@PathVariable String customerId, @PathVariable String depositId) {
        return ResponseEntity.ok(billingService.mobileMoneyDeposit(clientContextService.getAuthenticatedMember(), customerId, depositId));
    }

    @GetMapping("/{customerId}/payments/{transactionNumber}/receipt")
    public ResponseEntity<PaymentReceiptResponse> receipt(@PathVariable String customerId, @PathVariable String transactionNumber) {
        return ResponseEntity.ok(billingService.receipt(clientContextService.getAuthenticatedMember(), customerId, transactionNumber));
    }

    @GetMapping("/{customerId}/payments/{transactionNumber}/receipt/pdf")
    public ResponseEntity<byte[]> receiptPdf(@PathVariable String customerId, @PathVariable String transactionNumber) {
        return pdf("receipt-" + transactionNumber + ".pdf",
                billingService.receiptPdf(clientContextService.getAuthenticatedMember(), customerId, transactionNumber));
    }

    private static ResponseEntity<byte[]> pdf(String fileName, byte[] body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(body);
    }
}
