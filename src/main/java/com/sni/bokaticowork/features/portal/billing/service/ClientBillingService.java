package com.sni.bokaticowork.features.portal.billing.service;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.dto.response.CustomerStatementResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentPdfService;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.billing.service.support.BillingReceivables;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentIntentFromBillingDocumentRequest;
import com.sni.bokaticowork.features.payment.dto.request.InitiateMobileMoneyDepositRequest;
import com.sni.bokaticowork.features.payment.dto.request.WalletPaymentRequest;
import com.sni.bokaticowork.features.payment.security.service.WalletMerchantPaymentGuard;
import com.sni.bokaticowork.features.payment.security.enums.WalletOperationType;

import com.sni.bokaticowork.features.payment.model.PawapayDeposit;
import com.sni.bokaticowork.features.payment.cash.dto.request.DeclareCashPaymentRequest;
import com.sni.bokaticowork.features.payment.cash.dto.response.CashPaymentDeclarationResponse;
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
    private final WalletMerchantPaymentGuard merchantPaymentGuard;
    private final PaymentReceiptService paymentReceiptService;
    private final com.sni.bokaticowork.features.payment.repository.PawapayDepositRepository pawapayDepositRepository;
    private final com.sni.bokaticowork.features.payment.service.pawaypay.PawapayDepositService pawapayDepositService;
    private final com.sni.bokaticowork.features.payment.cash.service.CashPaymentDeclarationService cashDeclarationService;

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
        result.setData(source.getData().stream().filter(this::visibleToClient).map(this::toSummary).toList());
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

    /**
     * Ou en est le paiement mobile money que ce membre a lance.
     *
     * <p>Le suivi passait par la route d'administration, ouverte a tous : un identifiant de
     * depot suffisait a lire le numero et le montant de n'importe qui. Ici on verifie que le
     * depot appartient au membre connecte, et on ne lui dit rien d'autre que le sien.</p>
     */
    @Transactional(readOnly = true)
    public MobileMoneyDepositResponse mobileMoneyDeposit(Member member, String depositId) {
        PawapayDeposit deposit = pawapayDepositRepository.findByDepositId(depositId)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement mobile money introuvable : " + depositId));
        boolean owned = OWNER_TYPE.equalsIgnoreCase(deposit.getCustomerType())
                && member.getMemberId().equalsIgnoreCase(deposit.getCustomerCode());
        if (!owned) {
            // On ne distingue pas « ce depot n'existe pas » de « ce depot n'est pas le votre » ·
            // la difference renseignerait sur les paiements des autres.
            throw new ResourceNotFoundException("Paiement mobile money introuvable : " + depositId);
        }
        return pawapayDepositService.toResponse(deposit);
    }

    /**
     * « Je passerai payer en especes » · une annonce, pas un paiement.
     *
     * <p>Le mobile money et le portefeuille aboutissent seuls. Les especes arrivent avec la
     * personne · la facture reste donc due jusqu a ce que la caisse ait compte les billets.
     * L annonce sert a prevenir la caisse, et a tenir le creneau d une reservation.</p>
     */
    @Transactional
    public CashPaymentDeclarationResponse declareCashPayment(Member member, String documentNumber,
                                                             DeclareCashPaymentRequest request) {
        BillingDocumentResponse invoice = billingDocumentService.get(documentNumber);
        verifyOwnership(member, invoice);
        return cashDeclarationService.declare(documentNumber, request, member.getMemberId());
    }

    /** Les annonces de ce membre · celles en attente disent ce qu il reste a aller regler. */
    @Transactional(readOnly = true)
    public PaginatedResponse<CashPaymentDeclarationResponse> listCashDeclarations(Member member, Pageable pageable) {
        return cashDeclarationService.listForCustomer(OWNER_TYPE, member.getMemberId(), pageable);
    }

    /** Le client se ravise · sa facture reste due, elle n est simplement plus annoncee. */
    @Transactional
    public CashPaymentDeclarationResponse cancelCashDeclaration(Member member, String declarationNumber) {
        cashDeclarationService.ownedBy(declarationNumber, OWNER_TYPE, member.getMemberId());
        return cashDeclarationService.cancel(declarationNumber, "Annulée par le client", member.getMemberId());
    }

    @Transactional
    public PaymentTransactionResponse payWithWallet(Member member, String documentNumber, String pin) {
        BillingDocumentResponse invoice = billingDocumentService.get(documentNumber);
        verifyOwnership(member, invoice);
        WalletResponse wallet = walletService.getOrCreate(OWNER_TYPE, member.getMemberId(), invoice.currency());
        merchantPaymentGuard.assertAllowed(walletService.serviceWallet(wallet.walletNumber()),
                WalletOperationType.MERCHANT_PAYMENT, invoice.balanceDue(), pin);
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
        // Une facture en brouillon n'a jamais ete presentee au client · elle ne se compte ni dans
        // ce qu'il a recu, ni dans ce qu'il doit. Elle le faisait passer pour debiteur a tort.
        long invoiceCount = statement.documents().stream()
                .filter(d -> d.documentType() == BillingDocumentType.INVOICE
                        && BillingReceivables.issued(d.documentType(), d.status()))
                .count();
        long unpaidCount = statement.documents().stream()
                .filter(d -> d.documentType() == BillingDocumentType.INVOICE
                        && BillingReceivables.receivable(d.documentType(), d.status())
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
                .totalCreditAvailable(statement.totalCreditAvailable())
                .netBalanceDue(statement.netBalanceDue())
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
        // Une facture en brouillon est un document de travail · elle n'a pas ete emise, le client
        // ne la connait pas, et il n'a rien a devoir a son titre. Elle n'existe pas pour lui.
        if (invoice.status() == BillingDocumentStatus.DRAFT) {
            throw new ResourceNotFoundException("Invoice not found");
        }
    }

    /** Ce que le client peut voir · tout sauf ce qui n'a pas encore ete emis. */
    private boolean visibleToClient(BillingDocumentResponse document) {
        return document.status() != BillingDocumentStatus.DRAFT;
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
