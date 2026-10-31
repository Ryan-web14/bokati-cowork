package com.sni.bokaticowork.features.portal.company.service;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentPdfService;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.company.membership.service.CompanyMembershipService;
import com.sni.bokaticowork.features.company.statement.dto.StatementDtos;
import com.sni.bokaticowork.features.company.statement.service.AccountStatementService;
import com.sni.bokaticowork.features.payment.cash.dto.request.DeclareCashPaymentRequest;
import com.sni.bokaticowork.features.payment.cash.dto.response.CashPaymentDeclarationResponse;
import com.sni.bokaticowork.features.payment.cash.service.CashPaymentDeclarationService;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentIntentFromBillingDocumentRequest;
import com.sni.bokaticowork.features.payment.dto.request.InitiateMobileMoneyDepositRequest;
import com.sni.bokaticowork.features.payment.dto.response.MobileMoneyDepositResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentIntentResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentReceiptResponse;
import com.sni.bokaticowork.features.payment.model.PawapayDeposit;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.repository.PawapayDepositRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentReceiptService;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentService;
import com.sni.bokaticowork.features.payment.service.pawaypay.PawapayDepositService;
import com.sni.bokaticowork.features.portal.billing.dto.request.ClientInitiateMobileMoneyPaymentRequest;
import com.sni.bokaticowork.features.portal.billing.dto.response.ClientInvoiceResponse;
import com.sni.bokaticowork.features.portal.billing.service.ClientBillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Le compte de l'organisation dans le portail du gerant · son etat de compte, ses factures et
 * celles de ses membres qui lui sont reliees, et leur reglement.
 *
 * <p>Seul un gerant actif y accede ; pour tout autre membre l'organisation n'existe pas (404).
 * Une facture qui n'est ni a l'organisation ni reliee a elle repond aussi 404, sans dire qu'elle
 * existe. Le reglement se fait par mobile money, par annonce d'especes, ou par le portefeuille de
 * l'organisation ({@code CompanyWalletService}) ; le portefeuille personnel du gerant ne paie pas
 * pour l'organisation.</p>
 */
@Service
@RequiredArgsConstructor
public class ClientCompanyBillingService {

    private static final String BILLING_DOCUMENT_SOURCE = "BILLING_DOCUMENT";

    private final CompanyMembershipService membershipService;
    private final AccountStatementService statementService;
    private final BillingDocumentService billingDocumentService;
    private final BillingDocumentPdfService billingDocumentPdfService;
    private final ClientBillingService clientBillingService;
    private final PaymentService paymentService;
    private final PaymentReceiptService paymentReceiptService;
    private final PawapayDepositRepository pawapayDepositRepository;
    private final PawapayDepositService pawapayDepositService;
    private final CashPaymentDeclarationService cashDeclarationService;

    @Transactional(readOnly = true)
    public StatementDtos.Statement statement(Member owner, String customerId, LocalDate from, LocalDate to) {
        Customer company = membershipService.ownedCompany(owner, customerId);
        return statementService.company(company, from, to);
    }

    @Transactional(readOnly = true)
    public ClientInvoiceResponse invoice(Member owner, String customerId, String documentNumber) {
        Customer company = membershipService.ownedCompany(owner, customerId);
        return clientBillingService.toDetail(companyInvoice(company, documentNumber));
    }

    @Transactional(readOnly = true)
    public byte[] invoicePdf(Member owner, String customerId, String documentNumber) {
        Customer company = membershipService.ownedCompany(owner, customerId);
        companyInvoice(company, documentNumber);
        return billingDocumentPdfService.generatePdf(documentNumber);
    }

    @Transactional
    public MobileMoneyDepositResponse payWithMobileMoney(Member owner, String customerId, String documentNumber,
                                                         ClientInitiateMobileMoneyPaymentRequest request) {
        Customer company = membershipService.ownedCompany(owner, customerId);
        companyInvoice(company, documentNumber);
        PaymentIntentResponse intent = paymentService.createIntentFromBillingDocument(
                new CreatePaymentIntentFromBillingDocumentRequest(documentNumber, null, null, null, null));
        return paymentService.initiateMobileMoneyDeposit(intent.intentNumber(),
                new InitiateMobileMoneyDepositRequest(intent.intentNumber(), request.getPhoneNumber(),
                        request.getCorrespondent(), null, owner.getMemberId(), null));
    }

    /** Ou en est un paiement mobile money sur une facture de l'organisation. */
    @Transactional(readOnly = true)
    public MobileMoneyDepositResponse mobileMoneyDeposit(Member owner, String customerId, String depositId) {
        Customer company = membershipService.ownedCompany(owner, customerId);
        PawapayDeposit deposit = pawapayDepositRepository.findByDepositId(depositId)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement mobile money introuvable : " + depositId));
        PaymentIntent intent = deposit.getPaymentIntent();
        boolean owned = intent != null && BILLING_DOCUMENT_SOURCE.equalsIgnoreCase(intent.getSourceType())
                && belongsTo(company, billingDocumentService.get(intent.getSourceCode()));
        if (!owned) {
            throw new ResourceNotFoundException("Paiement mobile money introuvable : " + depositId);
        }
        return pawapayDepositService.toResponse(deposit);
    }

    /** « Je passerai payer en especes » pour l'organisation · la facture reste due jusqu'a la caisse. */
    @Transactional
    public CashPaymentDeclarationResponse declareCashPayment(Member owner, String customerId, String documentNumber,
                                                             DeclareCashPaymentRequest request) {
        Customer company = membershipService.ownedCompany(owner, customerId);
        companyInvoice(company, documentNumber);
        return cashDeclarationService.declare(documentNumber, request, owner.getMemberId());
    }

    @Transactional(readOnly = true)
    public PaymentReceiptResponse receipt(Member owner, String customerId, String transactionNumber) {
        Customer company = membershipService.ownedCompany(owner, customerId);
        PaymentReceiptResponse receipt = paymentReceiptService.getByTransactionNumber(transactionNumber);
        ensureReceiptOf(company, receipt);
        return receipt;
    }

    @Transactional(readOnly = true)
    public byte[] receiptPdf(Member owner, String customerId, String transactionNumber) {
        Customer company = membershipService.ownedCompany(owner, customerId);
        ensureReceiptOf(company, paymentReceiptService.getByTransactionNumber(transactionNumber));
        return paymentReceiptService.generatePdfByTransactionNumber(transactionNumber);
    }

    // ── Appartenance ─────────────────────────────────────────────────────────

    private BillingDocumentResponse companyInvoice(Customer company, String documentNumber) {
        BillingDocumentResponse invoice;
        try {
            invoice = billingDocumentService.get(documentNumber);
        } catch (ResourceNotFoundException ex) {
            throw new ResourceNotFoundException("Facture introuvable");
        }
        if (!belongsTo(company, invoice) || invoice.status() == BillingDocumentStatus.DRAFT) {
            throw new ResourceNotFoundException("Facture introuvable");
        }
        return invoice;
    }

    /** A l'organisation elle-meme, ou d'un membre et reliee a elle. */
    static boolean belongsTo(Customer company, BillingDocumentResponse document) {
        if (document == null || company.getCustomerId() == null) {
            return false;
        }
        boolean own = "CUSTOMER".equalsIgnoreCase(document.customerType())
                && company.getCustomerId().equalsIgnoreCase(document.customerCode());
        return own || company.getCustomerId().equalsIgnoreCase(document.companyCustomerCode());
    }

    private void ensureReceiptOf(Customer company, PaymentReceiptResponse receipt) {
        boolean own = "CUSTOMER".equalsIgnoreCase(receipt.customerType())
                && company.getCustomerId().equalsIgnoreCase(receipt.customerCode());
        boolean linked = false;
        if (!own && BILLING_DOCUMENT_SOURCE.equalsIgnoreCase(receipt.sourceType()) && receipt.sourceCode() != null) {
            try {
                linked = belongsTo(company, billingDocumentService.get(receipt.sourceCode()));
            } catch (ResourceNotFoundException ignored) {
                linked = false;
            }
        }
        if (!own && !linked) {
            throw new ResourceNotFoundException("Receipt not found");
        }
    }
}
