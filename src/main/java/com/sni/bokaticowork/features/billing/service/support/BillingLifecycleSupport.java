package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import org.springframework.stereotype.Component;

@Component
public class BillingLifecycleSupport {

    /** Statuts d'un document emis qui admettent le scellement (lot B1). */
    private static final java.util.Set<BillingDocumentStatus> VALIDATABLE = java.util.EnumSet.of(BillingDocumentStatus.ISSUED,
            BillingDocumentStatus.SENT, BillingDocumentStatus.PARTIALLY_PAID, BillingDocumentStatus.PAID, BillingDocumentStatus.OVERDUE,
            BillingDocumentStatus.APPLIED, BillingDocumentStatus.REFUNDED);

    /** Un avoir impute ou reverse est consomme : il ne s'utilise plus. */
    public static boolean creditNoteConsumed(BillingDocument creditNote) {
        return creditNote.getStatus() == BillingDocumentStatus.APPLIED || creditNote.getStatus() == BillingDocumentStatus.REFUNDED;
    }

    public void ensureCanIssue(BillingDocument document) {
        if (document.getStatus() != BillingDocumentStatus.DRAFT) {
            throw new BadRequestException("Only draft billing documents can be issued");
        }
    }

    public void ensureCanSend(BillingDocument document) {
        if (document.getStatus() == BillingDocumentStatus.DRAFT) {
            // Lot B1 : un brouillon s'emet avant de partir (controle des remises, numero, vendeur fige).
            throw new BadRequestException("Un brouillon s'émet avant d'être envoyé");
        }
        if (document.getStatus() == BillingDocumentStatus.CANCELLED
                || document.getStatus() == BillingDocumentStatus.REFUNDED) {
            throw new BadRequestException("Billing document cannot be sent in status " + document.getStatus());
        }
    }

    public void ensureCanPay(BillingDocument document) {
        // Lot B5 : une proforma ne s'encaisse pas directement ; son paiement devient un acompte du devis.
        if (document.getDocumentType() != BillingDocumentType.INVOICE) {
            throw new BadRequestException("Payments can only be allocated to invoices");
        }
        if (document.getStatus() == BillingDocumentStatus.CANCELLED
                || document.getStatus() == BillingDocumentStatus.REFUNDED) {
            throw new BadRequestException("Billing document cannot be paid in status " + document.getStatus());
        }
    }

    /**
     * Un avoir se fait sur une facture, une rectificative ou une note de debit (et une proforma, tant
     * qu'elle est encaissable). A distinguer de {@link #ensureCanPay}, reserve aux encaissements :
     * l'annulation d'une rectificative payee echouait sur « Payments can only be allocated to invoices ».
     */
    public void ensureCanCredit(BillingDocument document) {
        if (document.getDocumentType() != BillingDocumentType.INVOICE
                && document.getDocumentType() != BillingDocumentType.CORRECTIVE_INVOICE
                && document.getDocumentType() != BillingDocumentType.DEBIT_NOTE) {
            throw new BadRequestException("Un avoir se fait sur une facture, une rectificative ou une note de débit");
        }
        if (document.getStatus() == BillingDocumentStatus.CANCELLED
                || document.getStatus() == BillingDocumentStatus.REFUNDED) {
            throw new BadRequestException("Aucun avoir possible sur un document au statut " + document.getStatus());
        }
    }

    public void ensureQuoteCanConvert(BillingDocument quote) {
        if (quote.getDocumentType() != BillingDocumentType.QUOTE) {
            throw new BadRequestException("Billing document is not a quote");
        }
        // Lot B3 : un devis dont les acomptes sont demandes se convertit aussi ; la facture deduira ce
        // qui est encaisse et reclamera le reste.
        if (quote.getStatus() != BillingDocumentStatus.ACCEPTED
                && quote.getStatus() != BillingDocumentStatus.DEPOSIT_REQUESTED
                && quote.getStatus() != BillingDocumentStatus.DEPOSIT_PAID) {
            throw new BadRequestException("Un devis se convertit une fois accepté");
        }
    }

    public void ensureNotLocked(BillingDocument document) {
        if (Boolean.TRUE.equals(document.getLocked())) {
            throw new BadRequestException(
                    "Document " + document.getDocumentNumber() + " est validé (SEFC) · il ne peut plus être modifié");
        }
    }

    public void ensureCanValidate(BillingDocument document) {
        if (document.getDocumentType() != BillingDocumentType.INVOICE
                && document.getDocumentType() != BillingDocumentType.CREDIT_NOTE
                && document.getDocumentType() != BillingDocumentType.CORRECTIVE_INVOICE
                && document.getDocumentType() != BillingDocumentType.DEBIT_NOTE) {
            // Lot B5 : une proforma n'est pas une piece fiscale, elle ne se scelle pas.
            throw new BadRequestException("Seules les factures, avoirs, rectificatives et notes de débit peuvent être validés");
        }
        if (Boolean.TRUE.equals(document.getLocked())) {
            throw new BadRequestException("Document déjà validé");
        }
        if (document.getStatus() == BillingDocumentStatus.DRAFT) {
            // Lot B1 : la validation sautait l'emission et ses controles (remises, vendeur, notification).
            throw new BadRequestException("Un brouillon s'émet avant d'être validé");
        }
        if (!VALIDATABLE.contains(document.getStatus())) {
            throw new BadRequestException("Le document ne peut pas être validé au statut " + document.getStatus());
        }
    }
}
