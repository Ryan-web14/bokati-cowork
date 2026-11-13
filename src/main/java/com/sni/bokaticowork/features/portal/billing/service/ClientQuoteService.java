package com.sni.bokaticowork.features.portal.billing.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.billing.dto.ClaimDtos;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.service.implementation.ClaimService;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentPdfService;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentIntentFromBillingDocumentRequest;
import com.sni.bokaticowork.features.payment.dto.request.InitiateMobileMoneyDepositRequest;
import com.sni.bokaticowork.features.payment.dto.response.MobileMoneyDepositResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentIntentResponse;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentService;
import com.sni.bokaticowork.features.portal.billing.dto.request.ClientInitiateMobileMoneyPaymentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Lot C16 : devis, proformas et reclamations dans l'espace client (authentifie, pas de lien a
 * jeton). Le membre ne voit que ses pieces, jamais un brouillon ; il accepte ou refuse un devis
 * propose, paie une proforma par mobile money (le paiement devient un acompte du devis, lot B5),
 * ouvre ou retire une reclamation sur une de ses factures.
 */
@Service
@RequiredArgsConstructor
public class ClientQuoteService {

    private static final String OWNER_TYPE = "MEMBER";
    private static final Set<BillingDocumentStatus> ANSWERABLE = EnumSet.of(BillingDocumentStatus.ISSUED, BillingDocumentStatus.SENT,
            BillingDocumentStatus.NEGOTIATION);

    private final BillingDocumentService billingDocumentService;
    private final BillingDocumentPdfService pdfService;
    private final PaymentService paymentService;
    private final ClaimService claimService;
    private final ClientBillingService clientBillingService;

    @Transactional(readOnly = true)
    public PaginatedResponse<com.sni.bokaticowork.features.portal.billing.dto.response.ClientInvoiceSummaryResponse> list(
            Member member, BillingDocumentType type, Pageable pageable) {
        if (type != BillingDocumentType.QUOTE && type != BillingDocumentType.PROFORMA_INVOICE) {
            throw BadRequestException.coded("TYPE_INVALIDE", "type vaut QUOTE ou PROFORMA_INVOICE.");
        }
        PaginatedResponse<BillingDocumentResponse> source = billingDocumentService.list(type, null, OWNER_TYPE, member.getMemberId(),
                null, null, null, null, null, pageable);
        PaginatedResponse<com.sni.bokaticowork.features.portal.billing.dto.response.ClientInvoiceSummaryResponse> result = new PaginatedResponse<>();
        result.setData(source.getData().stream().filter(document -> document.status() != BillingDocumentStatus.DRAFT)
                .map(clientBillingService::toSummary).toList());
        result.setPageable(source.getPageable());
        return result;
    }

    @Transactional(readOnly = true)
    public com.sni.bokaticowork.features.portal.billing.dto.response.ClientInvoiceResponse get(Member member, String number) {
        return clientBillingService.toDetail(owned(member, number, null));
    }

    @Transactional(readOnly = true)
    public byte[] pdf(Member member, String number) {
        owned(member, number, null);
        return pdfService.generatePdf(number);
    }

    /** Le membre accepte le devis qui lui est propose. */
    @Transactional
    public com.sni.bokaticowork.features.portal.billing.dto.response.ClientInvoiceResponse accept(Member member, String number) {
        answerable(owned(member, number, BillingDocumentType.QUOTE));
        return clientBillingService.toDetail(billingDocumentService.acceptQuote(number));
    }

    /** Le membre refuse le devis. */
    @Transactional
    public com.sni.bokaticowork.features.portal.billing.dto.response.ClientInvoiceResponse reject(Member member, String number) {
        answerable(owned(member, number, BillingDocumentType.QUOTE));
        return clientBillingService.toDetail(billingDocumentService.rejectQuote(number));
    }

    /** Paiement d'une proforma par mobile money : il devient un acompte du devis issu d'elle (lot B5). */
    @Transactional
    public MobileMoneyDepositResponse payProforma(Member member, String number, ClientInitiateMobileMoneyPaymentRequest request) {
        owned(member, number, BillingDocumentType.PROFORMA_INVOICE);
        PaymentIntentResponse intent = paymentService.createIntentFromBillingDocument(
                new CreatePaymentIntentFromBillingDocumentRequest(number, null, null, null, null));
        return paymentService.initiateMobileMoneyDeposit(intent.intentNumber(), new InitiateMobileMoneyDepositRequest(intent.intentNumber(),
                request.getPhoneNumber(), request.getCorrespondent(), null, member.getMemberId(), null));
    }

    // ------------------------------------------------------------------ reclamations

    @Transactional
    public ClaimDtos.ClaimView openClaim(Member member, String invoiceNumber, ClaimDtos.OpenRequest request) {
        BillingDocumentResponse invoice = owned(member, invoiceNumber, null);
        if (invoice.documentType() == BillingDocumentType.QUOTE || invoice.documentType() == BillingDocumentType.PROFORMA_INVOICE) {
            throw BadRequestException.coded("RECLAMATION_IMPOSSIBLE", "Une réclamation porte sur une facture.");
        }
        return claimService.open(invoiceNumber, request, "PORTAL", member.getMemberId());
    }

    @Transactional(readOnly = true)
    public List<ClaimDtos.ClaimView> claims(Member member) {
        return claimService.list(null, OWNER_TYPE, member.getMemberId(), false);
    }

    @Transactional
    public ClaimDtos.ClaimView withdrawClaim(Member member, String claimNumber) {
        ClaimDtos.ClaimView claim = claimService.get(claimNumber);
        if (!OWNER_TYPE.equals(claim.customerType()) || !member.getMemberId().equals(claim.customerCode())) {
            throw new ResourceNotFoundException("Réclamation introuvable : " + claimNumber);
        }
        return claimService.withdraw(claimNumber);
    }

    @Transactional(readOnly = true)
    public List<ClaimDtos.ReasonView> claimReasons() {
        return claimService.reasons(false);
    }

    // ------------------------------------------------------------------ outils

    /** Piece du membre connecte, jamais un brouillon ; une autre piece n'existe pas pour lui. */
    private BillingDocumentResponse owned(Member member, String number, BillingDocumentType type) {
        BillingDocumentResponse document = billingDocumentService.get(number);
        if (!OWNER_TYPE.equals(document.customerType()) || !member.getMemberId().equals(document.customerCode())
                || document.status() == BillingDocumentStatus.DRAFT || (type != null && document.documentType() != type)) {
            throw new ResourceNotFoundException("Document introuvable : " + number);
        }
        return document;
    }

    private static void answerable(BillingDocumentResponse quote) {
        if (!ANSWERABLE.contains(quote.status())) {
            throw BadRequestException.coded("DEVIS_DEJA_REPONDU", "Ce devis n'attend plus de réponse (" + quote.status() + ").");
        }
    }
}
