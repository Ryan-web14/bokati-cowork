package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import org.springframework.stereotype.Component;

@Component
public class BillingLifecycleSupport {

    public void ensureCanIssue(BillingDocument document) {
        if (document.getStatus() != BillingDocumentStatus.DRAFT) {
            throw new BadRequestException("Only draft billing documents can be issued");
        }
    }

    public void ensureCanSend(BillingDocument document) {
        if (document.getStatus() == BillingDocumentStatus.CANCELLED
                || document.getStatus() == BillingDocumentStatus.VOIDED
                || document.getStatus() == BillingDocumentStatus.REFUNDED) {
            throw new BadRequestException("Billing document cannot be sent in status " + document.getStatus());
        }
    }

    public void ensureCanPay(BillingDocument document) {
        if (document.getDocumentType() != BillingDocumentType.INVOICE
                && document.getDocumentType() != BillingDocumentType.PROFORMA_INVOICE) {
            throw new BadRequestException("Payments can only be allocated to invoices");
        }
        if (document.getStatus() == BillingDocumentStatus.CANCELLED
                || document.getStatus() == BillingDocumentStatus.VOIDED
                || document.getStatus() == BillingDocumentStatus.REFUNDED) {
            throw new BadRequestException("Billing document cannot be paid in status " + document.getStatus());
        }
    }

    public void ensureQuoteCanConvert(BillingDocument quote) {
        if (quote.getDocumentType() != BillingDocumentType.QUOTE) {
            throw new BadRequestException("Billing document is not a quote");
        }
        if (quote.getStatus() != BillingDocumentStatus.ACCEPTED
                && quote.getStatus() != BillingDocumentStatus.DEPOSIT_PAID) {
            throw new BadRequestException("Quote must be accepted or have its deposit paid before conversion");
        }
    }

    public void ensureNotLocked(BillingDocument document) {
        if (Boolean.TRUE.equals(document.getLocked())) {
            throw new BadRequestException(
                    "Document " + document.getDocumentNumber() + " est validé (SEFC) — il ne peut plus être modifié");
        }
    }

    public void ensureCanValidate(BillingDocument document) {
        if (document.getDocumentType() != BillingDocumentType.INVOICE
                && document.getDocumentType() != BillingDocumentType.CREDIT_NOTE
                && document.getDocumentType() != BillingDocumentType.DEBIT_NOTE
                && document.getDocumentType() != BillingDocumentType.PROFORMA_INVOICE) {
            throw new BadRequestException("Seules les factures, avoirs et notes de débit peuvent être validés");
        }
        if (Boolean.TRUE.equals(document.getLocked())) {
            throw new BadRequestException("Document déjà validé");
        }
        if (document.getStatus() != BillingDocumentStatus.DRAFT
                && document.getStatus() != BillingDocumentStatus.ISSUED
                && document.getStatus() != BillingDocumentStatus.PAID) {
            throw new BadRequestException(
                    "Le document doit être en statut DRAFT, ISSUED ou PAID pour être validé, statut actuel : " + document.getStatus());
        }
    }
}
