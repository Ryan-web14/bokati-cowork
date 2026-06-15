package com.sni.bokaticowork.features.portal.billing.service;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.dto.response.CustomerStatementResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentPdfService;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentIntentFromBillingDocumentRequest;
import com.sni.bokaticowork.features.payment.dto.request.InitiateMobileMoneyDepositRequest;
import com.sni.bokaticowork.features.payment.dto.request.WalletPaymentRequest;
import com.sni.bokaticowork.features.payment.dto.response.MobileMoneyDepositResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentIntentResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentTransactionResponse;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentReceiptService;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentService;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletService;
import com.sni.bokaticowork.features.payment.dto.response.PaymentReceiptResponse;
import com.sni.bokaticowork.features.payment.dto.response.WalletResponse;
import com.sni.bokaticowork.features.payment.enums.PaymentIntentStatus;
import com.sni.bokaticowork.features.portal.billing.dto.request.ClientInitiateMobileMoneyPaymentRequest;
import com.sni.bokaticowork.features.portal.billing.dto.response.ClientInvoiceResponse;
import com.sni.bokaticowork.features.portal.billing.dto.response.ClientInvoiceSummaryResponse;
import com.sni.bokaticowork.features.portal.billing.dto.response.ClientSpendingSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ClientBillingService {

    private static final String OWNER_TYPE = "MEMBER";

    private final BillingDocumentService billingDocumentService;
    private final BillingDocumentPdfService billingDocumentPdfService;
    private final PaymentService paymentService;
    private final WalletService walletService;
    private final PaymentReceiptService paymentReceiptService;

    @Transactional(readOnly = true)
    public PaginatedResponse<ClientInvoiceSummaryResponse> listInvoices(Member member,
                                                                          BillingDocumentStatus status,
                                                                          LocalDate from,
                                                                          LocalDate to,
                                                                          Pageable pageable) {
        PaginatedResponse<BillingDocumentResponse> source = billingDocumentService.list(
                BillingDocumentType.INVOICE, status, OWNER_TYPE, member.getMemberId(),
                null, null, from, to, null, pageable
        );
        PaginatedResponse<ClientInvoiceSummaryResponse> result = new PaginatedResponse<>();
        result.setData(source.getData().stream().map(this::toSummary).toList());
        result.setPageable(source.getPageable());
        return result;
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<ClientInvoiceSummaryResponse> listPayableInvoices(Member member, Pageable pageable) {
        PaginatedResponse<BillingDocumentResponse> source = paymentService.listPayableDocuments(
                BillingDocumentType.INVOICE, OWNER_TYPE, member.getMemberId(), null, null, null, pageable
        );
        PaginatedResponse<ClientInvoiceSummaryResponse> result = new PaginatedResponse<>();
        result.setData(source.getData().stream().map(this::toSummary).toList());
        result.setPageable(source.getPageable());
        return result;
    }

    @Transactional(readOnly = true)
    public ClientInvoiceResponse getInvoice(Member member, String documentNumber) {
        BillingDocumentResponse invoice = billingDocumentService.get(documentNumber);
        verifyOwnership(member, invoice);
        return toDetail(invoice);
    }

    @Transactional(readOnly = true)
    public byte[] downloadInvoicePdf(Member member, String documentNumber) {
        BillingDocumentResponse invoice = billingDocumentService.get(documentNumber);
        verifyOwnership(member, invoice);
        return billingDocumentPdfService.generatePdf(documentNumber);
    }

    @Transactional
    public MobileMoneyDepositResponse payWithMobileMoney(Member member, String documentNumber,
                                                          ClientInitiateMobileMoneyPaymentRequest request) {
        BillingDocumentResponse invoice = billingDocumentService.get(documentNumber);
        verifyOwnership(member, invoice);
        PaymentIntentResponse intent = paymentService.createIntentFromBillingDocument(
                new CreatePaymentIntentFromBillingDocumentRequest(documentNumber, null, null, null, null)
        );
        return paymentService.initiateMobileMoneyDeposit(
                intent.intentNumber(),
                new InitiateMobileMoneyDepositRequest(
                        intent.intentNumber(),
                        request.getPhoneNumber(),
                        request.getCorrespondent(),
                        null,
                        member.getMemberId(),
                        null
                )
        );
    }

    @Transactional
    public PaymentTransactionResponse payWithWallet(Member member, String documentNumber) {
        BillingDocumentResponse invoice = billingDocumentService.get(documentNumber);
        verifyOwnership(member, invoice);
        WalletResponse wallet = walletService.getOrCreate(OWNER_TYPE, member.getMemberId(), invoice.currency());
        PaymentIntentResponse intent = paymentService.createIntentFromBillingDocument(
                new CreatePaymentIntentFromBillingDocumentRequest(documentNumber, null, null, null, null)
        );
        return paymentService.payWithWallet(
                intent.intentNumber(),
                new WalletPaymentRequest(wallet.walletNumber(), null, member.getMemberId(), null)
        );
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<PaymentIntentResponse> listPayments(Member member, Pageable pageable) {
        return paymentService.listIntents(
                null, OWNER_TYPE, member.getMemberId(), null, null, null, pageable
        );
    }

    @Transactional(readOnly = true)
    public PaymentReceiptResponse getReceipt(Member member, String transactionNumber) {
        PaymentReceiptResponse receipt = paymentReceiptService.getByTransactionNumber(transactionNumber);
        if (!OWNER_TYPE.equals(receipt.customerType()) || !member.getMemberId().equals(receipt.customerCode())) {
            throw new ResourceNotFoundException("Receipt not found");
        }
        return receipt;
    }

    @Transactional(readOnly = true)
    public byte[] downloadReceiptPdf(Member member, String transactionNumber) {
        PaymentReceiptResponse receipt = paymentReceiptService.getByTransactionNumber(transactionNumber);
        if (!OWNER_TYPE.equals(receipt.customerType()) || !member.getMemberId().equals(receipt.customerCode())) {
            throw new ResourceNotFoundException("Receipt not found");
        }
        return paymentReceiptService.generatePdfByTransactionNumber(transactionNumber);
    }

    @Transactional(readOnly = true)
    public ClientSpendingSummaryResponse getSpendingSummary(Member member) {
        CustomerStatementResponse statement = billingDocumentService.customerStatement(
                OWNER_TYPE, member.getMemberId(), Pageable.unpaged()
        );
        long invoiceCount = statement.documents().stream()
                .filter(d -> d.documentType() == BillingDocumentType.INVOICE)
                .count();
        long unpaidCount = statement.documents().stream()
                .filter(d -> d.documentType() == BillingDocumentType.INVOICE
                        && d.balanceDue() != null
                        && d.balanceDue().compareTo(BigDecimal.ZERO) > 0)
                .count();
        long overdueCount = statement.documents().stream()
                .filter(d -> d.documentType() == BillingDocumentType.INVOICE
                        && d.status() == BillingDocumentStatus.OVERDUE)
                .count();
        return ClientSpendingSummaryResponse.builder()
                .totalInvoiced(statement.totalInvoiced())
                .totalPaid(statement.totalPaid())
                .totalBalanceDue(statement.totalBalanceDue())
                .invoiceCount(invoiceCount)
                .unpaidCount(unpaidCount)
                .overdueCount(overdueCount)
                .build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void verifyOwnership(Member member, BillingDocumentResponse invoice) {
        if (!OWNER_TYPE.equals(invoice.customerType())
                || !member.getMemberId().equals(invoice.customerCode())) {
            throw new ResourceNotFoundException("Invoice not found");
        }
    }

    private ClientInvoiceSummaryResponse toSummary(BillingDocumentResponse d) {
        return ClientInvoiceSummaryResponse.builder()
                .documentNumber(d.documentNumber())
                .documentType(d.documentType())
                .status(d.status())
                .title(d.title())
                .currency(d.currency())
                .totalAmount(d.totalAmount())
                .paidAmount(d.paidAmount())
                .balanceDue(d.balanceDue())
                .issueDate(d.issueDate())
                .dueDate(d.dueDate())
                .issuedAt(d.issuedAt())
                .paidAt(d.paidAt())
                .sourceType(d.sourceType())
                .sourceCode(d.sourceCode())
                .resolvedSourceLabel(d.resolvedSourceLabel())
                .build();
    }

    private ClientInvoiceResponse toDetail(BillingDocumentResponse d) {
        return ClientInvoiceResponse.builder()
                .documentNumber(d.documentNumber())
                .documentType(d.documentType())
                .status(d.status())
                .title(d.title())
                .description(d.description())
                .customerName(d.customerName())
                .customerEmail(d.customerEmail())
                .currency(d.currency())
                .subtotalAmount(d.subtotalAmount())
                .discountAmount(d.discountAmount())
                .vatAmount(d.vatAmount())
                .totalAmount(d.totalAmount())
                .paidAmount(d.paidAmount())
                .balanceDue(d.balanceDue())
                .issueDate(d.issueDate())
                .dueDate(d.dueDate())
                .issuedAt(d.issuedAt())
                .paidAt(d.paidAt())
                .paymentReference(d.paymentReference())
                .paymentInstructions(d.paymentInstructions())
                .bankDetailsJson(d.bankDetailsJson())
                .sourceType(d.sourceType())
                .sourceCode(d.sourceCode())
                .resolvedSourceLabel(d.resolvedSourceLabel())
                .lines(d.lines())
                .build();
    }
}
