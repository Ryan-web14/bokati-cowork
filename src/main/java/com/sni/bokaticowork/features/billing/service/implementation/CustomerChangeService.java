package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.dto.request.ChangeCustomerRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateCreditNoteRequest;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.dto.response.ChangeCustomerResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingCustomerChange;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingCustomerChangeRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.support.BillingEventWriter;
import com.sni.bokaticowork.security.admin.user.service.interfaces.StaffProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Changement de client sur une facture (lot B6, decision D6).
 *
 * <p>Avant scellement, sans paiement ni ecriture : la meme facture passe au nouveau client, dans une
 * nouvelle version (fiche, conditions, exoneration reprises, recalcul). Sinon, en une action :
 * nouvelle facture identique au nouveau client ; avoir du reste du au client d'origine, impute sur
 * sa facture ; avoir des reglements recus, garde au client d'origine (a rembourser ou imputer) ou
 * transfere sur la nouvelle facture (piece 411 de l'un a l'autre, le payeur reel reste trace) ;
 * facture d'origine annulee, remplacee par la nouvelle.</p>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CustomerChangeService {

    static final String IN_PLACE = "IN_PLACE";
    static final String REISSUE = "REISSUE";
    static final String KEEP = "KEEP_AS_CREDIT";
    static final String TRANSFER = "TRANSFER";

    private final BillingDocumentServiceImpl billing;
    private final BillingDocumentRepository documents;
    private final BillingCustomerChangeRepository changes;
    private final BillingEventWriter eventWriter;
    private final StaffProfileService staffProfiles;

    public ChangeCustomerResponse change(String invoiceNumber, ChangeCustomerRequest request) {
        BillingDocument invoice = billing.serviceByNumber(invoiceNumber);
        if (invoice.getDocumentType() != BillingDocumentType.INVOICE || StringUtils.hasText(invoice.getDepositForDocumentNumber())) {
            throw new BadRequestException("Le changement de client se fait sur une facture");
        }
        if (invoice.getStatus() == BillingDocumentStatus.CANCELLED || StringUtils.hasText(invoice.getReplacedByDocumentNumber())) {
            throw new BadRequestException("Cette facture est annulée ou remplacée");
        }
        String toType = request.customerType().trim();
        String toCode = request.customerCode().trim();
        if (toType.equalsIgnoreCase(String.valueOf(invoice.getCustomerType())) && toCode.equals(invoice.getCustomerCode())) {
            throw new BadRequestException("La facture est déjà à ce client");
        }
        String reason = request.reason().trim();
        String actor = staffProfiles.currentAuthor().login();
        BigDecimal paid = invoice.getPaidAmount() == null ? BigDecimal.ZERO : invoice.getPaidAmount();
        BillingCustomerChange change = BillingCustomerChange.builder().originalDocumentNumber(invoiceNumber).fromType(invoice.getCustomerType())
                .fromCode(invoice.getCustomerCode()).toType(toType).toCode(toCode).reason(reason).createdBy(actor).build();
        boolean inPlace = !Boolean.TRUE.equals(invoice.getLocked()) && paid.signum() == 0 && !documents.salePosted(invoiceNumber);
        if (inPlace) {
            billing.changeCustomerInPlace(invoice, toType, toCode, reason);
            change.setMode(IN_PLACE);
            change.setNewDocumentNumber(invoiceNumber);
            changes.save(change);
            return new ChangeCustomerResponse(IN_PLACE, invoiceNumber, invoiceNumber, null, null, null, null);
        }

        // 1. Nouvelle facture identique au nouveau client, emise (et scellee si l'originale l'etait).
        BillingDocumentResponse created = billing.create(billing.reissueRequest(invoice, toType, toCode));
        billing.issue(created.documentNumber(), invoice.getDiscountOverrideReason());
        if (Boolean.TRUE.equals(invoice.getLocked())) {
            billing.validate(created.documentNumber());
        }
        String newNumber = created.documentNumber();
        // 2. Avoir du reste du, impute sur la facture d'origine.
        BigDecimal unpaid = invoice.getBalanceDue() == null ? BigDecimal.ZERO : invoice.getBalanceDue();
        String unpaidCredit = null;
        if (unpaid.signum() > 0) {
            unpaidCredit = billing.createCreditNote(invoiceNumber, new CreateCreditNoteRequest(unpaid,
                    "Changement de client (reste dû) : " + reason, true, true, null)).documentNumber();
        }
        // 3. Avoir des reglements recus : garde au client d'origine, ou transfere sur la nouvelle facture.
        String paymentsMode = StringUtils.hasText(request.payments()) ? request.payments() : KEEP;
        String paidCredit = null;
        BigDecimal transferred = null;
        if (paid.signum() > 0) {
            paidCredit = billing.createCreditNote(invoiceNumber, new CreateCreditNoteRequest(paid,
                    "Changement de client (règlements reçus) : " + reason, false, true, null)).documentNumber();
            if (TRANSFER.equals(paymentsMode)) {
                transferred = billing.transferCreditNote(paidCredit, newNumber, invoice.getCustomerCode());
            }
        }
        // 4. La facture d'origine est annulee, remplacee par la nouvelle (les avoirs portent la contre-passation).
        billing.cancelReplacedInvoice(invoiceNumber, newNumber);
        change.setMode(REISSUE);
        change.setNewDocumentNumber(newNumber);
        change.setUnpaidCreditNote(unpaidCredit);
        change.setPaidCreditNote(paidCredit);
        change.setPaymentsMode(paid.signum() > 0 ? paymentsMode : null);
        change.setTransferredAmount(transferred);
        changes.save(change);
        eventWriter.write(billing.serviceByNumber(invoiceNumber), "INVOICE_CUSTOMER_CHANGED", Map.of("newInvoice", newNumber, "reason", reason,
                "paymentsMode", paid.signum() > 0 ? paymentsMode : "NONE"));
        return new ChangeCustomerResponse(REISSUE, invoiceNumber, newNumber, unpaidCredit, paidCredit, change.getPaymentsMode(), transferred);
    }
}
