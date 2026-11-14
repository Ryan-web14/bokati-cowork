package com.sni.bokaticowork.features.document.template.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.accounting.service.implementation.BankService;
import com.sni.bokaticowork.features.accounting.service.implementation.ChequeService;
import com.sni.bokaticowork.features.accounting.service.implementation.LoanService;
import com.sni.bokaticowork.features.accounting.service.implementation.PurchaseService;
import com.sni.bokaticowork.features.accounting.service.implementation.StatementPrintService;
import com.sni.bokaticowork.features.accounting.service.implementation.VatService;
import com.sni.bokaticowork.features.document.template.render.DocumentData;
import com.sni.bokaticowork.features.fund.service.FundNeedService;
import com.sni.bokaticowork.features.fund.service.FundRequestService;
import com.sni.bokaticowork.features.fund.voucher.FundVoucherSamples;
import com.sni.bokaticowork.features.fund.voucher.FundVoucherService;
import com.sni.bokaticowork.features.fund.voucher.VoucherService;
import com.sni.bokaticowork.features.payment.service.interfaces.CashDocumentPdfService;
import com.sni.bokaticowork.features.payment.service.support.CashReportDocumentData;
import com.sni.bokaticowork.features.payment.service.support.CashVoucherDocumentData;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.Set;

/**
 * Donnees des pieces pour l'apercu des modeles (lots D2 et D4) : une piece d'exemple, ou une piece
 * reelle designee par son numero, lue sans rien ecrire.
 *
 * <p>Pour les pieces comptables produites par {@link VoucherService}, la piece reelle est imprimee
 * dans une transaction annulee, en mode capture : le numero attribue et la trace d'impression ne
 * sont pas gardes.</p>
 */
@Component
public class DocumentDataSources {

    private static final Set<String> CASH = Set.of(CashVoucherDocumentData.CASH_OUT, CashVoucherDocumentData.CASH_IN,
            CashReportDocumentData.REQUEST, CashReportDocumentData.CLOSING);
    private static final Set<String> FUND = Set.of("NEED_STATEMENT", "FUND_REQUEST", "DISBURSEMENT_VOUCHER", "JUSTIFICATION_SHEET");
    private static final Set<String> ACCOUNTING = Set.of("PURCHASE_ORDER", "VAT_RETURN", "BANK_RECONCILIATION", "LOAN_SCHEDULE",
            "BANK_DEPOSIT_SLIP", "SUPPLIER_STATEMENT", "PARTNER_STATEMENT");
    private static final Set<String> CLIENT = Set.of("INVOICE", "CREDIT_NOTE", "PAYMENT_RECEIPT", "WALLET_STATEMENT", "WALLET_RECEIPT",
            "BOOKING_CONFIRMATION");
    /** Numero attendu pour l'apercu avec une piece reelle. */
    private static final Map<String, String> HINTS = Map.ofEntries(
            Map.entry(CashVoucherDocumentData.CASH_OUT, "Numéro du mouvement de caisse (CSM-...)"),
            Map.entry(CashVoucherDocumentData.CASH_IN, "Numéro du mouvement de caisse (CSM-...)"),
            Map.entry(CashReportDocumentData.REQUEST, "Numéro de la demande de caisse (CRQ-...)"),
            Map.entry(CashReportDocumentData.CLOSING, "Numéro de la session de caisse (CSS-...)"),
            Map.entry("NEED_STATEMENT", "Numéro de l'état de besoin (EDB-...)"),
            Map.entry("FUND_REQUEST", "Numéro de la demande de fonds (DDF-...)"),
            Map.entry("DISBURSEMENT_VOUCHER", "DEMANDE/MOUVEMENT (DDF-.../CSM-...)"),
            Map.entry("JUSTIFICATION_SHEET", "Numéro de la demande d'avance (DDF-...)"),
            Map.entry("PURCHASE_ORDER", "Numéro de la commande (BC-...)"),
            Map.entry("VAT_RETURN", "Identifiant de la déclaration de TVA"),
            Map.entry("BANK_RECONCILIATION", "Identifiant de l'état de rapprochement"),
            Map.entry("LOAN_SCHEDULE", "Code de l'emprunt"),
            Map.entry("BANK_DEPOSIT_SLIP", "Numéro du bordereau (BRB-...)"),
            Map.entry("SUPPLIER_STATEMENT", "Code du fournisseur"),
            Map.entry("PARTNER_STATEMENT", "Code du partenaire"),
            Map.entry("INVOICE", "Numéro de la pièce de facturation (INV-..., DEV-...)"),
            Map.entry("CREDIT_NOTE", "Numéro de l'avoir"),
            Map.entry("PAYMENT_RECEIPT", "Numéro du reçu (RCP-...)"));

    private final ObjectProvider<CashDocumentPdfService> cash;
    private final ObjectProvider<FundVoucherService> fund;
    private final ObjectProvider<FundNeedService> needs;
    private final ObjectProvider<FundRequestService> requests;
    private final ObjectProvider<PurchaseService> purchases;
    private final ObjectProvider<VatService> vat;
    private final ObjectProvider<BankService> bank;
    private final ObjectProvider<LoanService> loans;
    private final ObjectProvider<ChequeService> cheques;
    private final ObjectProvider<StatementPrintService> statements;
    private final ObjectProvider<PlatformTransactionManager> transactions;
    private ObjectProvider<com.sni.bokaticowork.features.billing.service.implementation.BillingDocumentPdfServiceImpl> invoices;
    private ObjectProvider<com.sni.bokaticowork.features.payment.service.implementation.PaymentReceiptServiceImpl> receipts;

    /** Pieces clients (lot D5) ; mutateur pour garder le constructeur des lots precedents. */
    @org.springframework.beans.factory.annotation.Autowired
    void setClientSources(ObjectProvider<com.sni.bokaticowork.features.billing.service.implementation.BillingDocumentPdfServiceImpl> invoices,
                          ObjectProvider<com.sni.bokaticowork.features.payment.service.implementation.PaymentReceiptServiceImpl> receipts) {
        this.invoices = invoices;
        this.receipts = receipts;
    }

    public DocumentDataSources(ObjectProvider<CashDocumentPdfService> cash, ObjectProvider<FundVoucherService> fund,
                               ObjectProvider<FundNeedService> needs, ObjectProvider<FundRequestService> requests,
                               ObjectProvider<PurchaseService> purchases, ObjectProvider<VatService> vat, ObjectProvider<BankService> bank,
                               ObjectProvider<LoanService> loans, ObjectProvider<ChequeService> cheques,
                               ObjectProvider<StatementPrintService> statements, ObjectProvider<PlatformTransactionManager> transactions) {
        this.cash = cash;
        this.fund = fund;
        this.needs = needs;
        this.requests = requests;
        this.purchases = purchases;
        this.vat = vat;
        this.bank = bank;
        this.loans = loans;
        this.cheques = cheques;
        this.statements = statements;
        this.transactions = transactions;
    }

    // ------------------------------------------------------------------ pieces des champs saisis (lot D3)

    /** Modele d'un mouvement de caisse : bon de sortie ou d'entree ; un autre mouvement n'a pas de champs. */
    public String cashTemplate(String movementNumber) {
        String code = cash.getObject().templateCode(movementNumber);
        if (code == null) {
            throw BadRequestException.coded("AUCUN_CHAMP", "Le mouvement " + movementNumber + " n'est ni une entrée ni une sortie de caisse");
        }
        return code;
    }

    /** Verifie que l'etat de besoin existe (introuvable : 404). */
    public void requireNeed(String number) {
        needs.getObject().get(number);
    }

    /** Verifie que la demande de fonds existe (introuvable : 404). */
    public void requireRequest(String number) {
        requests.getObject().get(number);
    }

    // ------------------------------------------------------------------ apercu

    public boolean supports(String code) {
        return CASH.contains(code) || FUND.contains(code) || ACCOUNTING.contains(code) || CLIENT.contains(code);
    }

    public String hint(String code) {
        return HINTS.get(code);
    }

    public DocumentData sample(String code) {
        if (CashReportDocumentData.REQUEST.equals(code)) {
            return CashReportDocumentData.sampleRequest();
        }
        if (CashReportDocumentData.CLOSING.equals(code)) {
            return CashReportDocumentData.sampleClosing();
        }
        if (CASH.contains(code)) {
            return CashVoucherDocumentData.sample(code);
        }
        if (FUND.contains(code)) {
            return FundVoucherSamples.sample(code);
        }
        if (ACCOUNTING.contains(code)) {
            return FundVoucherSamples.accounting(code);
        }
        switch (code) {
            case "INVOICE", "CREDIT_NOTE" -> {
                return com.sni.bokaticowork.features.billing.service.support.BillingDocumentData.sample(code);
            }
            case "PAYMENT_RECEIPT" -> {
                return com.sni.bokaticowork.features.payment.service.support.PaymentReceiptDocumentData.sample();
            }
            case "WALLET_STATEMENT" -> {
                return ClientDocumentSamples.walletStatement();
            }
            case "WALLET_RECEIPT" -> {
                return ClientDocumentSamples.walletReceipt();
            }
            case "BOOKING_CONFIRMATION" -> {
                return ClientDocumentSamples.bookingConfirmation();
            }
            default -> {
            }
        }
        throw BadRequestException.coded("APERCU_INDISPONIBLE", "Aucune donnée d'exemple pour le modèle " + code);
    }

    public DocumentData load(String code, String number) {
        String key = number.trim();
        if (CASH.contains(code)) {
            return cash.getObject().previewData(code, key);
        }
        if (FUND.contains(code)) {
            return fund.getObject().previewData(code, key);
        }
        return switch (code) {
            case "PURCHASE_ORDER" -> capture(() -> purchases.getObject().orderPdf(key));
            case "VAT_RETURN" -> capture(() -> vat.getObject().pdf(id(key)));
            case "BANK_RECONCILIATION" -> capture(() -> bank.getObject().pdf(id(key)));
            case "LOAN_SCHEDULE" -> capture(() -> loans.getObject().pdf(key));
            case "BANK_DEPOSIT_SLIP" -> capture(() -> cheques.getObject().slipPdf(key));
            case "SUPPLIER_STATEMENT" -> capture(() -> statements.getObject().supplierStatement(key, null, null));
            case "PARTNER_STATEMENT" -> capture(() -> statements.getObject().partnerStatement(key, null, null));
            case "INVOICE", "CREDIT_NOTE" -> invoices.getObject().templateData(key);
            case "PAYMENT_RECEIPT" -> receipts.getObject().templateData(key);
            default -> throw BadRequestException.coded("APERCU_INDISPONIBLE", "L'aperçu avec une pièce réelle n'existe pas pour le modèle " + code);
        };
    }

    /** Impression de la piece dans une transaction annulee, ses donnees capturees au lieu du PDF. */
    private DocumentData capture(Runnable print) {
        // Transaction propre : l'apercu tourne en lecture seule, et l'impression ecrit (numero, trace).
        TransactionTemplate template = new TransactionTemplate(transactions.getObject());
        template.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
        return template.execute(status -> {
            status.setRollbackOnly();
            return VoucherService.capturing(print);
        });
    }

    private static Long id(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            throw BadRequestException.coded("APERCU_INDISPONIBLE", "Identifiant numérique attendu : " + value);
        }
    }
}
