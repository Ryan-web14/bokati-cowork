package com.sni.bokaticowork.features.accounting.service.implementation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.features.accounting.dto.AccountingResponses;
import com.sni.bokaticowork.features.accounting.dto.PostingDtos;
import com.sni.bokaticowork.features.accounting.enums.AccountSide;
import com.sni.bokaticowork.features.accounting.enums.EntryStatus;
import com.sni.bokaticowork.features.accounting.repository.AccEntryLineRepository;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentLineRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.payment.enums.PaymentMethod;
import com.sni.bokaticowork.features.payment.enums.WalletEntryDirection;
import com.sni.bokaticowork.features.payment.enums.WalletEntryType;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.model.PaymentAllocation;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.model.WalletLedgerEntry;
import com.sni.bokaticowork.features.payment.repository.CashSessionRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentAllocationRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import com.sni.bokaticowork.features.payment.repository.WalletLedgerEntryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ventes et encaissements (lot F4, catalogue sections 1, 3, 4 et 5) : factures, avoirs,
 * annulations, encaissements par moyen, remboursements, mouvements de portefeuille.
 *
 * <p>Les tables de facturation, de paiement et de portefeuille ne sont pas modifiees (le grand
 * livre du portefeuille est en ajout seul) : le registre des evenements comptables dit ce qui est
 * deja transmis. Le lettrage automatique des comptes clients se fait par numero de facture.</p>
 *
 * <p>Le portefeuille est une dette envers le client : le solde de {@code 4191} egale la somme des
 * soldes des portefeuilles. Les transactions passent par le compte de liaison {@code 4719}
 * (rechargement, trop-percu, paiement par portefeuille) et chaque ecriture du grand livre du
 * portefeuille passe le {@code 4191} du client contre ce compte.</p>
 */
@Slf4j
@Service
public class SalesAccountingService implements com.sni.bokaticowork.features.accounting.service.support.PostingPayloadSource {

    public static final String INVOICE_ISSUED = "INVOICE_ISSUED";
    public static final String CREDIT_NOTE_ISSUED = "CREDIT_NOTE_ISSUED";
    public static final String INVOICE_CANCELLED = "INVOICE_CANCELLED";
    public static final String PAYMENT_RECEIVED = "PAYMENT_RECEIVED";
    public static final String PAYMENT_REFUNDED = "PAYMENT_REFUNDED";
    public static final String WALLET_ENTRY = "WALLET_ENTRY";
    public static final String CREDIT_NOTE_APPLIED = "CREDIT_NOTE_APPLIED";
    /** Lot K1 (A13) : facture finale annulee sans avoir, l'acompte deduit redevient une avance. */
    public static final String DEPOSIT_APPLICATION_CANCELLED = "DEPOSIT_APPLICATION_CANCELLED";
    /** Lot K1 (A13) : nouvelle facture d'un changement de client annulee sans avoir, le reglement revient au client d'origine. */
    public static final String RECEIVABLE_TRANSFER_CANCELLED = "RECEIVABLE_TRANSFER_CANCELLED";
    /** Lot K0 (M6) : types que ce service sait relire depuis la facturation, les paiements et le portefeuille. */
    static final Set<String> SOURCE_TYPES = Set.of(INVOICE_ISSUED, CREDIT_NOTE_ISSUED, INVOICE_CANCELLED, PAYMENT_RECEIVED, PAYMENT_REFUNDED,
            WALLET_ENTRY, CREDIT_NOTE_APPLIED, "DEPOSIT_APPLIED", "RECEIVABLE_TRANSFER", "WITHHOLDING_RECORDED", "WITHHOLDING_CANCELLED",
            "SECURITY_DEPOSIT_MOVEMENT", "LATE_ALLOCATION", "ALLOCATION_REVERSAL", DEPOSIT_APPLICATION_CANCELLED, RECEIVABLE_TRANSFER_CANCELLED);

    static final Set<BillingDocumentType> INVOICES = EnumSet.of(BillingDocumentType.INVOICE, BillingDocumentType.CORRECTIVE_INVOICE,
            BillingDocumentType.DEBIT_NOTE);

    private final BillingDocumentRepository documentRepository;
    private final BillingDocumentLineRepository documentLineRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final PaymentAllocationRepository allocationRepository;
    private final WalletLedgerEntryRepository ledgerRepository;
    private final CashSessionRepository sessionRepository;
    private final AccEntryLineRepository lineRepository;
    private final CashAccountingService cash;
    private final AccountingLetteringService lettering;
    private final AccountingChartService chart;
    private final AccountingSettings settings;
    private final PostingEngine engine;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate newTransaction;
    private final ExchangeRateService rates;

    public SalesAccountingService(BillingDocumentRepository documentRepository, BillingDocumentLineRepository documentLineRepository,
                                  PaymentTransactionRepository transactionRepository, PaymentAllocationRepository allocationRepository,
                                  WalletLedgerEntryRepository ledgerRepository, CashSessionRepository sessionRepository,
                                  AccEntryLineRepository lineRepository, CashAccountingService cash, AccountingLetteringService lettering,
                                  AccountingChartService chart, AccountingSettings settings, @Lazy PostingEngine engine, ObjectMapper objectMapper,
                                  DataSource dataSource, PlatformTransactionManager transactionManager, ExchangeRateService rates) {
        this.rates = rates;
        this.documentRepository = documentRepository;
        this.documentLineRepository = documentLineRepository;
        this.transactionRepository = transactionRepository;
        this.allocationRepository = allocationRepository;
        this.ledgerRepository = ledgerRepository;
        this.sessionRepository = sessionRepository;
        this.lineRepository = lineRepository;
        this.cash = cash;
        this.lettering = lettering;
        this.chart = chart;
        this.settings = settings;
        this.engine = engine;
        this.objectMapper = objectMapper;
        this.jdbc = new JdbcTemplate(dataSource);
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    // ------------------------------------------------------------------ alimentation

    public PostingDtos.BatchResult feed() {
        if (!settings.enabled()) {
            return new PostingDtos.BatchResult(0, 0, 0, List.of());
        }
        int batch = settings.postingBatchSize();
        Counter counter = new Counter();
        if (settings.familyEnabled("SALES")) {
            jdbc.queryForList("SELECT d.document_number FROM billing_document d WHERE d.document_type IN ('INVOICE', 'CORRECTIVE_INVOICE', 'DEBIT_NOTE', 'CREDIT_NOTE') "
                    + "AND d.issued_at IS NOT NULL AND d.deleted_at IS NULL AND d.document_number IS NOT NULL AND NOT EXISTS (SELECT 1 FROM acc_posting_event e "
                    + "WHERE e.event_type = CASE WHEN d.document_type = 'CREDIT_NOTE' THEN 'CREDIT_NOTE_ISSUED' ELSE 'INVOICE_ISSUED' END "
                    + "AND e.reference = d.document_number) ORDER BY d.issued_at LIMIT ?", String.class, batch)
                    .forEach(number -> counter.run(null, number, () -> feedDocument(number)));
            // Une facture annulee par avoir est deja contre-passee par l'avoir : pas de seconde contre-passation.
            jdbc.queryForList("SELECT d.document_number FROM billing_document d WHERE d.status IN ('CANCELLED', 'VOIDED') AND d.document_number IS NOT NULL "
                    + "AND NOT EXISTS (SELECT 1 FROM billing_document c WHERE c.document_type = 'CREDIT_NOTE' AND c.original_document_number = d.document_number "
                    + "AND c.status NOT IN ('DRAFT', 'CANCELLED') AND c.deleted_at IS NULL) "
                    + "AND EXISTS (SELECT 1 FROM acc_posting_event e WHERE e.event_type IN ('INVOICE_ISSUED', 'CREDIT_NOTE_ISSUED') AND e.reference = d.document_number) "
                    + "AND NOT EXISTS (SELECT 1 FROM acc_posting_event e WHERE e.event_type = 'INVOICE_CANCELLED' AND e.reference = d.document_number) "
                    + "ORDER BY d.cancelled_at LIMIT ?", String.class, batch)
                    .forEach(number -> counter.run(INVOICE_CANCELLED, number, () -> feedCancellation(number)));
        }
        if (settings.familyEnabled("SALES")) {
            // Lot K0 (A5) : avoir impute sur une autre facture (du meme client ou de son entreprise), une
            // fois l'avoir et la facture cible comptabilises.
            jdbc.queryForList("SELECT c.document_number FROM billing_document c WHERE c.document_type = 'CREDIT_NOTE' "
                    + "AND c.applied_to_document_number IS NOT NULL "
                    + "AND EXISTS (SELECT 1 FROM acc_posting_event e WHERE e.event_type = 'CREDIT_NOTE_ISSUED' AND e.reference = c.document_number AND e.status = 'POSTED') "
                    + "AND EXISTS (SELECT 1 FROM acc_posting_event e WHERE e.event_type = 'INVOICE_ISSUED' AND e.reference = c.applied_to_document_number AND e.status = 'POSTED') "
                    + "AND NOT EXISTS (SELECT 1 FROM acc_posting_event e WHERE e.event_type = 'CREDIT_NOTE_APPLIED' AND e.reference = c.document_number) "
                    + "ORDER BY c.applied_at LIMIT ?", String.class, batch)
                    .forEach(number -> counter.run(CREDIT_NOTE_APPLIED, number, () -> feedCreditNoteApplied(number)));
        }
        if (settings.familyEnabled("SALES")) {
            // Lot B3 : acomptes deduits d'une facture finale deja comptabilisee, une piece par acompte.
            jdbc.queryForList("SELECT dp.deposit_invoice_number FROM billing_deposit dp WHERE dp.status = 'DEDUCTED' AND dp.deposit_invoice_number IS NOT NULL "
                    + "AND EXISTS (SELECT 1 FROM acc_posting_event e WHERE e.event_type = 'INVOICE_ISSUED' AND e.reference = dp.deducted_on) "
                    + "AND NOT EXISTS (SELECT 1 FROM acc_posting_event e WHERE e.event_type = 'DEPOSIT_APPLIED' AND e.reference = dp.deposit_invoice_number) "
                    + "ORDER BY dp.deducted_at LIMIT ?", String.class, batch)
                    .forEach(number -> counter.run("DEPOSIT_APPLIED", number, () -> feedDepositApplied(number)));
        }
        if (settings.familyEnabled("SALES")) {
            // Lot K1 (A13) : la facture finale qui avait deduit l'acompte est annulee sans avoir : la
            // deduction est contre-passee, l'acompte redevient une avance au 4191 (avant : piece orpheline,
            // 4191 a zero et client crediteur au 411).
            jdbc.queryForList("SELECT dp.deposit_invoice_number FROM billing_deposit dp JOIN billing_document d ON d.document_number = dp.deducted_on "
                    + "WHERE d.status IN ('CANCELLED', 'VOIDED') AND dp.deposit_invoice_number IS NOT NULL "
                    + "AND NOT EXISTS (SELECT 1 FROM billing_document c WHERE c.document_type = 'CREDIT_NOTE' AND c.original_document_number = d.document_number "
                    + "AND c.status NOT IN ('DRAFT', 'CANCELLED') AND c.deleted_at IS NULL) "
                    + "AND EXISTS (SELECT 1 FROM acc_posting_event e WHERE e.event_type = 'DEPOSIT_APPLIED' AND e.reference = dp.deposit_invoice_number) "
                    + "AND NOT EXISTS (SELECT 1 FROM acc_posting_event e WHERE e.event_type = 'DEPOSIT_APPLICATION_CANCELLED' AND e.reference = dp.deposit_invoice_number) "
                    + "ORDER BY d.cancelled_at LIMIT ?", String.class, batch)
                    .forEach(number -> counter.run(DEPOSIT_APPLICATION_CANCELLED, number, () -> feedDepositApplicationCancelled(number)));
            // Lot K1 (A13) : la nouvelle facture d'un changement de client est annulee sans avoir : le
            // transfert de reglement est contre-passe, le reglement revient au client d'origine.
            jdbc.queryForList("SELECT DISTINCT c.new_document_number FROM billing_customer_change c JOIN billing_document d ON d.document_number = c.new_document_number "
                    + "WHERE d.status IN ('CANCELLED', 'VOIDED') "
                    + "AND NOT EXISTS (SELECT 1 FROM billing_document n WHERE n.document_type = 'CREDIT_NOTE' AND n.original_document_number = d.document_number "
                    + "AND n.status NOT IN ('DRAFT', 'CANCELLED') AND n.deleted_at IS NULL) "
                    + "AND EXISTS (SELECT 1 FROM acc_posting_event e WHERE e.event_type = 'RECEIVABLE_TRANSFER' AND e.reference = c.new_document_number) "
                    + "AND NOT EXISTS (SELECT 1 FROM acc_posting_event e WHERE e.event_type = 'RECEIVABLE_TRANSFER_CANCELLED' AND e.reference = c.new_document_number) "
                    + "LIMIT ?", String.class, batch)
                    .forEach(number -> counter.run(RECEIVABLE_TRANSFER_CANCELLED, number, () -> feedReceivableTransferCancelled(number)));
        }
        if (settings.familyEnabled("SALES")) {
            // Lot B6 : reglement transfere d'un client a l'autre lors d'un changement de client (D6).
            jdbc.queryForList("SELECT c.new_document_number FROM billing_customer_change c WHERE c.payments_mode = 'TRANSFER' "
                    + "AND c.transferred_amount > 0 AND c.paid_credit_note IS NOT NULL "
                    + "AND EXISTS (SELECT 1 FROM acc_posting_event e WHERE e.event_type = 'INVOICE_ISSUED' AND e.reference = c.new_document_number) "
                    + "AND NOT EXISTS (SELECT 1 FROM acc_posting_event e WHERE e.event_type = 'RECEIVABLE_TRANSFER' AND e.reference = c.new_document_number) "
                    + "ORDER BY c.created_at LIMIT ?", String.class, batch)
                    .forEach(number -> counter.run("RECEIVABLE_TRANSFER", number, () -> feedReceivableTransfer(number)));
        }
        if (settings.familyEnabled("SALES")) {
            // Lot C6 : retenues a la source sur des factures deja comptabilisees ; une retenue annulee
            // avant d'etre comptabilisee ne l'est jamais, une retenue comptabilisee puis annulee est contre-passee.
            jdbc.queryForList("SELECT w.withholding_number FROM billing_withholding w WHERE w.status = 'RECORDED' "
                    + "AND EXISTS (SELECT 1 FROM acc_posting_event e WHERE e.event_type = 'INVOICE_ISSUED' AND e.reference = w.document_number) "
                    + "AND NOT EXISTS (SELECT 1 FROM acc_posting_event e WHERE e.event_type = 'WITHHOLDING_RECORDED' AND e.reference = w.withholding_number) "
                    + "ORDER BY w.created_at LIMIT ?", String.class, batch)
                    .forEach(number -> counter.run("WITHHOLDING_RECORDED", number, () -> feedWithholding(number)));
            jdbc.queryForList("SELECT w.withholding_number FROM billing_withholding w WHERE w.status = 'CANCELLED' "
                    + "AND EXISTS (SELECT 1 FROM acc_posting_event e WHERE e.event_type = 'WITHHOLDING_RECORDED' AND e.reference = w.withholding_number) "
                    + "AND NOT EXISTS (SELECT 1 FROM acc_posting_event e WHERE e.event_type = 'WITHHOLDING_CANCELLED' AND e.reference = w.withholding_number) "
                    + "ORDER BY w.cancelled_at LIMIT ?", String.class, batch)
                    .forEach(number -> counter.run("WITHHOLDING_CANCELLED", number, () -> feedWithholdingCancellation(number)));
        }
        if (settings.familyEnabled("SALES")) {
            // Lot C7 : mouvements de caution (restitution, retenue, imputation), une fois la facture de la caution comptabilisee.
            jdbc.queryForList("SELECT m.movement_number FROM billing_security_deposit_movement m JOIN billing_security_deposit d ON d.id = m.deposit_id "
                    + "WHERE EXISTS (SELECT 1 FROM acc_posting_event e WHERE e.event_type = 'INVOICE_ISSUED' AND e.reference = d.document_number) "
                    + "AND m.wallet_settled = FALSE "
                    + "AND NOT EXISTS (SELECT 1 FROM acc_posting_event e WHERE e.event_type = 'SECURITY_DEPOSIT_MOVEMENT' AND e.reference = m.movement_number) "
                    + "ORDER BY m.created_at LIMIT ?", String.class, batch)
                    .forEach(number -> counter.run("SECURITY_DEPOSIT_MOVEMENT", number, () -> feedSecurityDepositMovement(number)));
        }
        if (settings.familyEnabled("PAYMENT")) {
            // Lot C9 : affectation apres coup d'une somme en attente, une fois le paiement comptabilise.
            jdbc.queryForList("SELECT a.id FROM payment_allocation a JOIN payment_transaction t ON t.id = a.payment_transaction_id "
                    + "WHERE a.allocation_mode = 'LATE' "
                    + "AND EXISTS (SELECT 1 FROM acc_posting_event e WHERE e.event_type = 'PAYMENT_RECEIVED' AND e.reference = t.transaction_number) "
                    + "AND NOT EXISTS (SELECT 1 FROM acc_posting_event e WHERE e.event_type = 'LATE_ALLOCATION' AND e.reference = CONCAT('ALLOC-', a.id)) "
                    + "ORDER BY a.allocated_at LIMIT ?", Long.class, batch)
                    .forEach(id -> counter.run("LATE_ALLOCATION", "ALLOC-" + id, () -> feedLateAllocation(id)));
        }
        if (settings.familyEnabled("PAYMENT")) {
            // Lot R4 : affectation annulee alors qu'elle etait comptabilisee, la somme revient en attente.
            jdbc.queryForList("SELECT r.id FROM payment_allocation_reversal r WHERE r.accounting_required "
                    + "AND NOT EXISTS (SELECT 1 FROM acc_posting_event e WHERE e.event_type = 'ALLOCATION_REVERSAL' AND e.reference = CONCAT('UNALLOC-', r.id)) "
                    + "ORDER BY r.reversed_at LIMIT ?", Long.class, batch)
                    .forEach(id -> counter.run("ALLOCATION_REVERSAL", "UNALLOC-" + id, () -> feedAllocationReversal(id)));
        }
        if (settings.familyEnabled("PAYMENT")) {
            Instant settled = Instant.now().minusSeconds(60L * Math.max(0, settings.paymentDelayMinutes()));
            jdbc.queryForList("SELECT t.transaction_number FROM payment_transaction t WHERE t.status IN ('SUCCEEDED', 'REFUNDED', 'REVERSED') "
                    + "AND COALESCE(t.paid_at, t.created_at) <= ? AND NOT EXISTS (SELECT 1 FROM acc_posting_event e "
                    + "WHERE e.event_type IN ('PAYMENT_RECEIVED', 'PAYMENT_REFUNDED') AND e.reference = t.transaction_number) "
                    + "ORDER BY COALESCE(t.paid_at, t.created_at) LIMIT ?", String.class, java.sql.Timestamp.from(settled), batch)
                    .forEach(number -> counter.run(PAYMENT_RECEIVED, number, () -> feedTransaction(number)));
        }
        if (settings.familyEnabled("WALLET")) {
            jdbc.queryForList("SELECT w.entry_number FROM wallet_ledger_entry w WHERE w.entry_type NOT IN ('HOLD', 'HOLD_RELEASE') "
                    + "AND NOT EXISTS (SELECT 1 FROM acc_posting_event e WHERE e.event_type = 'WALLET_ENTRY' AND e.reference = w.entry_number) "
                    + "ORDER BY w.created_at LIMIT ?", String.class, batch)
                    .forEach(number -> counter.run(WALLET_ENTRY, number, () -> feedWalletEntry(number)));
        }
        return counter.result();
    }

    private final class Counter {
        int processed;
        int ok;
        final List<String> errors = new ArrayList<>();

        void run(String eventType, String reference, Runnable action) {
            processed++;
            try {
                newTransaction.executeWithoutResult(status -> action.run());
                ok++;
            } catch (RuntimeException exception) {
                String reason = PostingEngine.readable(exception);
                errors.add(reference + " : " + reason);
                // Lot K0 (M3) : le fait passe en echec avec son motif ; il quitte la tete de la file des
                // faits a lire et se retente par « Regenerer » une fois la cause corrigee.
                String type = eventType != null ? eventType : documentEventType(reference);
                if (type != null) {
                    engine.recordFeedFailure(type, reference, familyOf(type), null, reason);
                }
            }
        }

        PostingDtos.BatchResult result() {
            return new PostingDtos.BatchResult(processed, ok, processed - ok, errors);
        }
    }

    void feedDocument(String number) {
        BillingDocument document = documentRepository.findByDocumentNumber(number).orElseThrow();
        boolean creditNote = document.getDocumentType() == BillingDocumentType.CREDIT_NOTE;
        LocalDate date = document.getIssueDate() != null ? document.getIssueDate() : date(document.getIssuedAt());
        // Document en devise : converti en XAF au taux du document, sinon au taux du jour d'emission.
        Conversion fx = conversion(document.getCurrency(), document.getExchangeRate(), date);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("documentNumber", document.getDocumentNumber());
        data.put("documentType", document.getDocumentType().name());
        data.put("customerCode", customerCode(document));
        data.put("customerName", document.getCustomerName());
        data.put("journalCode", settings.text("accounting.sales.journal", "VT"));
        data.put("clientAccount", settings.text("accounting.sales.client-account", "411"));
        data.put("discountAccount", settings.text("accounting.sales.discount-account", "709"));
        data.put("vatAccount", settings.text("accounting.sales.vat-account", "4431"));
        data.put("extraTaxAccount", settings.text("accounting.sales.extra-tax-account", "4431"));
        BigDecimal taxable = fx.apply(money(document.getTaxableAmount()));
        BigDecimal vat = fx.apply(money(document.getVatAmount()));
        BigDecimal extra = fx.apply(money(document.getTaxAmount()).subtract(money(document.getVatAmount())).max(BigDecimal.ZERO));
        // Part hors taxe non taxable (ligne exoneree, organisation exoneree, caution) : elle reste en
        // produit. Sans elle, la remise valait « brut moins base taxable » et comptait toute ligne
        // non taxable comme une remise : l'ecriture n'etait plus equilibree.
        BigDecimal nonTaxable = fx.apply(money(document.getTotalAmount()).subtract(money(document.getTaxableAmount()))
                .subtract(money(document.getTaxAmount())).max(BigDecimal.ZERO));
        List<Map<String, Object>> lines = new ArrayList<>();
        BigDecimal gross = BigDecimal.ZERO;
        for (BillingDocumentLine line : documentLineRepository.findAllByDocumentOrderByLineOrderAscIdAsc(document)) {
            BigDecimal amount = fx.apply(money(line.getSubtotalAmount() != null ? line.getSubtotalAmount() : line.getTaxableAmount()));
            if (amount.signum() == 0 || Boolean.TRUE.equals(line.getOptional())) {
                continue;
            }
            List<com.sni.bokaticowork.features.billing.service.implementation.QuoteTemplateService.Share> shares =
                    bundles == null ? List.of() : bundles.split(line.getItemCode(), amount);
            if (!shares.isEmpty()) {
                // Lot C14 : article compose, produit ventile entre ses composants (compte de chacun).
                for (var share : shares) {
                    Map<String, Object> part = new LinkedHashMap<>();
                    part.put("account", "INVENTORY_ITEM".equals(share.componentSource()) ? settings.text("accounting.sales.account.PRODUCT", "701")
                            : productAccount(BillingDocumentLine.builder().itemCode(share.componentCode()).lineType(line.getLineType())
                            .document(line.getDocument()).build()));
                    part.put("amount", share.amount());
                    part.put("label", line.getDescription());
                    lines.add(part);
                }
                gross = gross.add(amount);
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            String account = productAccount(line);
            item.put("account", account);
            item.put("amount", amount);
            item.put("label", line.getDescription());
            // Lot K0 (A2) : une ligne sur un compte de tiers (caution 165, debours) porte le client ;
            // sans lui, la piece partait en attente d'equilibrage.
            if (thirdPartyAccount(account)) {
                item.put("thirdParty", customerCode(document));
            }
            lines.add(item);
            gross = gross.add(amount);
        }
        data.put("lines", lines);
        // Remise : ecart entre le brut des lignes et la base taxable (remises de lignes et de document).
        BigDecimal reduction = gross.subtract(taxable).subtract(nonTaxable);
        // Lot K0 (A8) : une « remise » negative au-dela des arrondis de conversion (1 XAF par ligne) n'est
        // pas une remise mais une facture incoherente : elle n'est plus passee au 709 en silence.
        BigDecimal tolerance = BigDecimal.valueOf(Math.max(1, lines.size()));
        if (reduction.signum() < 0 && reduction.abs().compareTo(tolerance) > 0) {
            throw new IllegalStateException("Facture " + number + " incohérente : ses lignes totalisent " + gross.stripTrailingZeros().toPlainString()
                    + " XAF hors taxes, moins que sa base taxable et sa part non taxable (" + taxable.add(nonTaxable).stripTrailingZeros().toPlainString()
                    + " XAF). Corrigez la facture, puis régénérez l'événement.");
        }
        data.put("reduction", reduction);
        data.put("vatAmount", vat);
        data.put("extraTaxAmount", extra);
        // En devise, le total est la somme des parties converties : la piece reste equilibree.
        data.put("totalAmount", fx.foreign() ? taxable.add(nonTaxable).add(vat).add(extra) : money(document.getTotalAmount()));
        if (fx.foreign()) {
            data.put("currency", fx.currency());
            data.put("exchangeRate", fx.rate());
            data.put("foreignTotal", money(document.getTotalAmount()));
        }
        data.put("dueDate", document.getDueDate() == null ? null : document.getDueDate().toString());
        // Lot B3 : facture d'acompte, au credit du 4191 (schema SALES_DEPOSIT_INVOICE).
        data.put("depositFor", document.getDepositForDocumentNumber());
        data.put("depositAccount", settings.text("accounting.sales.deposit-account", "4191"));
        data.put("letteringReference", creditNote && StringUtils.hasText(document.getOriginalDocumentNumber())
                ? document.getOriginalDocumentNumber() : document.getDocumentNumber());
        data.put("originalDocumentNumber", document.getOriginalDocumentNumber());
        // Lot F11 : code de taxe et base sur les lignes de TVA ; un avoir rattache sa TVA a la facture d'origine.
        data.put("vatCode", vatCode(document, vat));
        data.put("extraTaxCode", extra.signum() > 0 ? settings.text("accounting.vat.extra-tax-code", "CA") : null);
        data.put("vatBase", taxable);
        data.put("vatReference", data.get("letteringReference"));
        // Lot K0 (A4) : avoir sur une facture d'acompte, il solde le 4191 et non un produit.
        if (creditNote && StringUtils.hasText(document.getOriginalDocumentNumber()) && !StringUtils.hasText(document.getDepositForDocumentNumber())) {
            documentRepository.findByDocumentNumber(document.getOriginalDocumentNumber())
                    .map(BillingDocument::getDepositForDocumentNumber).filter(StringUtils::hasText)
                    .ifPresent(depositFor -> data.put("depositFor", depositFor));
        }
        if (paymentRegime()) {
            // Regime des encaissements : la TVA attend en 4438 jusqu'au reglement. Un avoir reprend
            // l'attente de sa facture tant qu'elle la couvre, sinon la TVA deja exigible.
            String pending = settings.text("accounting.vat.pending-account", "4438");
            if (!creditNote || pendingRemaining((String) data.get("letteringReference")).compareTo(vat.add(extra)) >= 0) {
                data.put("vatAccount", pending);
                data.put("extraTaxAccount", pending);
            }
        }
        // Lot K0 (A7) : une ligne de TVA par taux, chacune avec son code et sa base.
        data.put("vatLines", vatLines(document, fx, vat, taxable, (String) data.get("vatAccount")));
        engine.receive(creditNote ? CREDIT_NOTE_ISSUED : INVOICE_ISSUED, number, "SALES", date, json(data), replay(date));
    }

    /**
     * Lot K0 (A7) : TVA ventilee par taux. Le montant de TVA du document est reparti au prorata de la
     * TVA des lignes de chaque taux (remises de document comprises) ; le dernier taux prend le reste,
     * pour que la somme egale la TVA du document.
     */
    List<Map<String, Object>> vatLines(BillingDocument document, Conversion fx, BigDecimal vat, BigDecimal taxable, String account) {
        if (vat.signum() == 0) {
            return List.of();
        }
        Map<BigDecimal, BigDecimal[]> byRate = new java.util.TreeMap<>();
        for (BillingDocumentLine line : documentLineRepository.findAllByDocumentOrderByLineOrderAscIdAsc(document)) {
            if (Boolean.TRUE.equals(line.getOptional()) || line.getVatRate() == null || line.getVatRate().signum() <= 0) {
                continue;
            }
            BigDecimal rate = line.getVatRate().compareTo(BigDecimal.ONE) < 0 ? line.getVatRate().movePointRight(2) : line.getVatRate();
            BigDecimal[] sums = byRate.computeIfAbsent(rate.stripTrailingZeros(), ignored -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            sums[0] = sums[0].add(money(line.getVatAmount()));
            sums[1] = sums[1].add(money(line.getTaxableAmount()));
        }
        List<Map<String, Object>> result = new ArrayList<>();
        if (byRate.size() <= 1) {
            Map<String, Object> single = new LinkedHashMap<>();
            single.put("account", account);
            single.put("amount", vat);
            single.put("base", taxable);
            single.put("taxCode", vatCode(document, vat));
            single.put("rateLabel", byRate.isEmpty() ? "" : byRate.keySet().iterator().next().toPlainString() + " %");
            result.add(single);
            return result;
        }
        BigDecimal lineVat = byRate.values().stream().map(sums -> sums[0]).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal lineBase = byRate.values().stream().map(sums -> sums[1]).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal vatLeft = vat;
        BigDecimal baseLeft = taxable;
        int index = 0;
        for (Map.Entry<BigDecimal, BigDecimal[]> rate : byRate.entrySet()) {
            index++;
            boolean last = index == byRate.size();
            BigDecimal share = last || lineVat.signum() == 0 ? vatLeft
                    : vat.multiply(rate.getValue()[0]).divide(lineVat, 0, RoundingMode.HALF_UP);
            BigDecimal base = last || lineBase.signum() == 0 ? baseLeft
                    : taxable.multiply(rate.getValue()[1]).divide(lineBase, 0, RoundingMode.HALF_UP);
            vatLeft = vatLeft.subtract(share);
            baseLeft = baseLeft.subtract(base);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("account", account);
            item.put("amount", money(share));
            item.put("base", money(base));
            item.put("taxCode", "TVA" + rate.getKey().toPlainString());
            item.put("rateLabel", rate.getKey().toPlainString() + " %");
            result.add(item);
        }
        return result;
    }

    /** Lot K0 (A5) : avoir impute sur une autre facture, D 411 du titulaire de l'avoir / C 411 du client de la facture. */
    void feedCreditNoteApplied(String creditNoteNumber) {
        BillingDocument creditNote = documentRepository.findByDocumentNumber(creditNoteNumber).orElseThrow();
        BillingDocument target = documentRepository.findByDocumentNumber(creditNote.getAppliedToDocumentNumber()).orElseThrow();
        LocalDate date = date(creditNote.getAppliedAt() == null ? Instant.now() : creditNote.getAppliedAt());
        Conversion fx = conversion(creditNote.getCurrency(), creditNote.getExchangeRate(), date);
        BigDecimal value = money(creditNote.getTotalAmount());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("creditNoteNumber", creditNoteNumber);
        data.put("documentNumber", target.getDocumentNumber());
        data.put("holderCode", customerCode(creditNote));
        data.put("customerCode", customerCode(target));
        // L'avoir est au credit du 411 du titulaire sous la reference de sa facture d'origine.
        data.put("creditNoteReference", StringUtils.hasText(creditNote.getOriginalDocumentNumber()) ? creditNote.getOriginalDocumentNumber() : creditNoteNumber);
        data.put("journalCode", settings.text("accounting.sales.journal", "VT"));
        data.put("clientAccount", settings.text("accounting.sales.client-account", "411"));
        data.put("amount", fx.apply(value));
        // Regime des encaissements : la compensation regle la facture cible, sa TVA devient exigible.
        data.put("vatTransfers", paymentRegime() ? vatTransfers(target.getDocumentNumber(), value, false) : List.of());
        data.put("vatPendingAccount", settings.text("accounting.vat.pending-account", "4438"));
        engine.receive(CREDIT_NOTE_APPLIED, creditNoteNumber, "SALES", date, json(data), replay(date));
    }

    // ------------------------------------------------------------------ relecture (lot K0, M6)

    @Override
    public boolean supports(String eventType) {
        return eventType != null && SOURCE_TYPES.contains(eventType.toUpperCase(java.util.Locale.ROOT));
    }

    @Override
    public void rebuild(String eventType, String reference) {
        newTransaction.executeWithoutResult(status -> {
            switch (eventType.toUpperCase(java.util.Locale.ROOT)) {
                case INVOICE_ISSUED, CREDIT_NOTE_ISSUED -> feedDocument(reference);
                case INVOICE_CANCELLED -> feedCancellation(reference);
                case PAYMENT_RECEIVED, PAYMENT_REFUNDED -> feedTransaction(reference);
                case WALLET_ENTRY -> feedWalletEntry(reference);
                case CREDIT_NOTE_APPLIED -> feedCreditNoteApplied(reference);
                case "DEPOSIT_APPLIED" -> feedDepositApplied(reference);
                case "RECEIVABLE_TRANSFER" -> feedReceivableTransfer(reference);
                case "WITHHOLDING_RECORDED" -> feedWithholding(reference);
                case "WITHHOLDING_CANCELLED" -> feedWithholdingCancellation(reference);
                case "SECURITY_DEPOSIT_MOVEMENT" -> feedSecurityDepositMovement(reference);
                case "LATE_ALLOCATION" -> feedLateAllocation(Long.parseLong(reference.substring("ALLOC-".length())));
                case "ALLOCATION_REVERSAL" -> feedAllocationReversal(Long.parseLong(reference.substring("UNALLOC-".length())));
                case DEPOSIT_APPLICATION_CANCELLED -> feedDepositApplicationCancelled(reference);
                case RECEIVABLE_TRANSFER_CANCELLED -> feedReceivableTransferCancelled(reference);
                default -> throw new IllegalStateException("Type " + eventType + " non relisible");
            }
        });
    }

    /** Type d'evenement d'un document de facturation (facture ou avoir), pour enregistrer un echec de lecture. */
    private String documentEventType(String number) {
        return documentRepository.findByDocumentNumber(number)
                .map(document -> document.getDocumentType() == BillingDocumentType.CREDIT_NOTE ? CREDIT_NOTE_ISSUED : INVOICE_ISSUED)
                .orElse(null);
    }

    private static String familyOf(String type) {
        return switch (type) {
            case PAYMENT_RECEIVED, PAYMENT_REFUNDED -> "PAYMENT";
            case WALLET_ENTRY -> "WALLET";
            default -> "SALES";
        };
    }

    private boolean thirdPartyAccount(String account) {
        return account != null && chart.find(account).map(found -> found.getThirdPartyType() != null).orElse(false);
    }

    /** Acompte deduit de la facture finale : D 4191 / C 411, TVA en attente liberee au prorata (lot B3). */
    void feedDepositApplied(String depositInvoiceNumber) {
        Map<String, Object> deposit = jdbc.queryForMap("SELECT dp.deducted_on, dp.deducted_amount, dp.deducted_at FROM billing_deposit dp "
                + "WHERE dp.deposit_invoice_number = ?", depositInvoiceNumber);
        String finalNumber = (String) deposit.get("deducted_on");
        BillingDocument document = documentRepository.findByDocumentNumber(finalNumber).orElseThrow();
        LocalDate date = date(deposit.get("deducted_at") instanceof java.sql.Timestamp at ? at.toInstant() : Instant.now());
        Conversion fx = conversion(document.getCurrency(), document.getExchangeRate(), date);
        BigDecimal value = money((BigDecimal) deposit.get("deducted_amount"));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("documentNumber", finalNumber);
        data.put("depositInvoiceNumber", depositInvoiceNumber);
        data.put("customerCode", customerCode(document));
        data.put("journalCode", settings.text("accounting.sales.journal", "VT"));
        data.put("clientAccount", settings.text("accounting.sales.client-account", "411"));
        data.put("depositAccount", settings.text("accounting.sales.deposit-account", "4191"));
        data.put("amount", fx.apply(value));
        data.put("vatTransfers", paymentRegime() ? vatTransfers(finalNumber, value, false) : List.of());
        data.put("vatPendingAccount", settings.text("accounting.vat.pending-account", "4438"));
        engine.receive("DEPOSIT_APPLIED", depositInvoiceNumber, "SALES", date, json(data), replay(date));
    }

    /** Lot K1 (A13) : contre-passation de la deduction d'un acompte, la facture finale etant annulee. */
    void feedDepositApplicationCancelled(String depositInvoiceNumber) {
        Map<String, Object> row = jdbc.queryForMap("SELECT dp.deducted_on, d.cancelled_at FROM billing_deposit dp "
                + "JOIN billing_document d ON d.document_number = dp.deducted_on WHERE dp.deposit_invoice_number = ?", depositInvoiceNumber);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("depositInvoiceNumber", depositInvoiceNumber);
        data.put("_reverse", Map.of("eventType", "DEPOSIT_APPLIED", "reference", depositInvoiceNumber,
                "reason", "Facture finale " + row.get("deducted_on") + " annulée : l'acompte redevient une avance"));
        LocalDate date = date(row.get("cancelled_at") instanceof java.sql.Timestamp at ? at.toInstant() : Instant.now());
        engine.receive(DEPOSIT_APPLICATION_CANCELLED, depositInvoiceNumber, "SALES", date, json(data), replay(date));
    }

    /** Lot K1 (A13) : contre-passation d'un transfert de reglement, la nouvelle facture etant annulee. */
    void feedReceivableTransferCancelled(String newDocumentNumber) {
        BillingDocument document = documentRepository.findByDocumentNumber(newDocumentNumber).orElseThrow();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("documentNumber", newDocumentNumber);
        data.put("_reverse", Map.of("eventType", "RECEIVABLE_TRANSFER", "reference", newDocumentNumber,
                "reason", "Facture " + newDocumentNumber + " annulée : le règlement revient au client d'origine"));
        LocalDate date = date(document.getCancelledAt() == null ? Instant.now() : document.getCancelledAt());
        engine.receive(RECEIVABLE_TRANSFER_CANCELLED, newDocumentNumber, "SALES", date, json(data), replay(date));
    }

    /** Lot B6 : reglement du client d'origine reporte sur la nouvelle facture, D 411 origine / C 411 nouveau client. */
    void feedReceivableTransfer(String newDocumentNumber) {
        Map<String, Object> change = jdbc.queryForMap("SELECT c.paid_credit_note, c.from_code, c.to_code, c.transferred_amount, c.created_at "
                + "FROM billing_customer_change c WHERE c.new_document_number = ? ORDER BY c.created_at DESC LIMIT 1", newDocumentNumber);
        BillingDocument document = documentRepository.findByDocumentNumber(newDocumentNumber).orElseThrow();
        BillingDocument creditNote = documentRepository.findByDocumentNumber((String) change.get("paid_credit_note")).orElseThrow();
        LocalDate date = date(change.get("created_at") instanceof java.sql.Timestamp at ? at.toInstant() : Instant.now());
        Conversion fx = conversion(document.getCurrency(), document.getExchangeRate(), date);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("documentNumber", newDocumentNumber);
        data.put("creditNoteNumber", creditNote.getDocumentNumber());
        data.put("fromCustomer", customerCode(creditNote));
        data.put("toCustomer", customerCode(document));
        data.put("journalCode", settings.text("accounting.sales.journal", "VT"));
        data.put("clientAccount", settings.text("accounting.sales.client-account", "411"));
        data.put("amount", fx.apply(money((BigDecimal) change.get("transferred_amount"))));
        // Lot K0 (A6) : le reglement transfere solde la nouvelle facture, sa TVA en attente devient exigible.
        data.put("vatTransfers", paymentRegime() ? vatTransfers(newDocumentNumber, money((BigDecimal) change.get("transferred_amount")), false) : List.of());
        data.put("vatPendingAccount", settings.text("accounting.vat.pending-account", "4438"));
        engine.receive("RECEIVABLE_TRANSFER", newDocumentNumber, "SALES", date, json(data), replay(date));
    }

    /**
     * Lot C7 : mouvement de caution. D 165 ; C 411 du client (restitution : le client devient
     * crediteur, rembourse par les circuits habituels ; imputation : lettre avec la facture cible)
     * ou C produit (retenue).
     */
    void feedSecurityDepositMovement(String movementNumber) {
        Map<String, Object> row = jdbc.queryForMap("SELECT m.kind, m.amount, m.target_document_number, m.movement_date, d.deposit_number, "
                + "d.document_number FROM billing_security_deposit_movement m JOIN billing_security_deposit d ON d.id = m.deposit_id "
                + "WHERE m.movement_number = ?", movementNumber);
        BillingDocument invoice = documentRepository.findByDocumentNumber((String) row.get("document_number")).orElseThrow();
        LocalDate date = row.get("movement_date") instanceof java.sql.Date day ? day.toLocalDate() : LocalDate.now();
        Conversion fx = conversion(invoice.getCurrency(), invoice.getExchangeRate(), date);
        String kind = (String) row.get("kind");
        boolean retained = "RETAIN".equals(kind);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("depositNumber", row.get("deposit_number"));
        data.put("kindLabel", retained ? "Retenue" : "APPLY".equals(kind) ? "Imputation" : "Restitution");
        data.put("journalCode", settings.text("accounting.sales.journal", "VT"));
        data.put("securityDepositAccount", settings.text("accounting.sales.security-deposit-account", "165"));
        data.put("counterAccount", retained ? settings.text("accounting.sales.deposit-retention-account", "758")
                : settings.text("accounting.sales.client-account", "411"));
        data.put("thirdParty", retained ? null : customerCode(invoice));
        // Lot K0 (A2) : le 165 est suivi par client, sa ligne porte toujours le client.
        data.put("depositThirdParty", customerCode(invoice));
        data.put("reference", "APPLY".equals(kind) ? row.get("target_document_number") : retained ? movementNumber : invoice.getDocumentNumber());
        data.put("amount", fx.apply(money((BigDecimal) row.get("amount"))));
        engine.receive("SECURITY_DEPOSIT_MOVEMENT", movementNumber, "SALES", date, json(data), replay(date));
    }

    /** Lot C9 : affectation apres coup, D 4719 / C 411 du client (lettre avec la facture). */
    void feedLateAllocation(Long allocationId) {
        Map<String, Object> row = jdbc.queryForMap("SELECT a.billing_document_number, a.allocated_amount, a.allocated_at, t.transaction_number "
                + "FROM payment_allocation a JOIN payment_transaction t ON t.id = a.payment_transaction_id WHERE a.id = ?", allocationId);
        BillingDocument document = documentRepository.findByDocumentNumber((String) row.get("billing_document_number")).orElseThrow();
        LocalDate date = date(row.get("allocated_at") instanceof java.sql.Timestamp at ? at.toInstant() : Instant.now());
        Conversion fx = conversion(document.getCurrency(), document.getExchangeRate(), date);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("transactionNumber", row.get("transaction_number"));
        data.put("documentNumber", document.getDocumentNumber());
        data.put("customerCode", customerCode(document));
        data.put("journalCode", settings.text("accounting.sales.journal", "VT"));
        data.put("clientAccount", settings.text("accounting.sales.client-account", "411"));
        data.put("unallocatedAccount", settings.text("accounting.sales.unallocated-account", "4719"));
        data.put("amount", fx.apply(money((BigDecimal) row.get("allocated_amount"))));
        // Lot K0 (A6) : regime des encaissements, la facture reglee apres coup libere sa TVA en attente.
        data.put("vatTransfers", paymentRegime() ? vatTransfers(document.getDocumentNumber(), money((BigDecimal) row.get("allocated_amount")), false) : List.of());
        data.put("vatPendingAccount", settings.text("accounting.vat.pending-account", "4438"));
        engine.receive("LATE_ALLOCATION", "ALLOC-" + allocationId, "SALES", date, json(data), replay(date));
    }

    /** Lot R4 : affectation annulee, D 411 du client (la facture redevient due) / C 4719. */
    void feedAllocationReversal(Long reversalId) {
        Map<String, Object> row = jdbc.queryForMap("SELECT transaction_number, document_number, amount, reversed_at "
                + "FROM payment_allocation_reversal WHERE id = ?", reversalId);
        BillingDocument document = documentRepository.findByDocumentNumber((String) row.get("document_number")).orElseThrow();
        LocalDate date = date(row.get("reversed_at") instanceof java.sql.Timestamp at ? at.toInstant() : Instant.now());
        Conversion fx = conversion(document.getCurrency(), document.getExchangeRate(), date);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("transactionNumber", row.get("transaction_number"));
        data.put("documentNumber", document.getDocumentNumber());
        data.put("customerCode", customerCode(document));
        data.put("journalCode", settings.text("accounting.sales.journal", "VT"));
        data.put("clientAccount", settings.text("accounting.sales.client-account", "411"));
        data.put("unallocatedAccount", settings.text("accounting.sales.unallocated-account", "4719"));
        data.put("amount", fx.apply(money((BigDecimal) row.get("amount"))));
        // Lot K0 (A6) : l'affectation annulee remet en attente la TVA qu'elle avait liberee.
        data.put("vatTransfers", paymentRegime() ? vatTransfers(document.getDocumentNumber(), money((BigDecimal) row.get("amount")), true) : List.of());
        data.put("vatPendingAccount", settings.text("accounting.vat.pending-account", "4438"));
        engine.receive("ALLOCATION_REVERSAL", "UNALLOC-" + reversalId, "SALES", date, json(data), replay(date));
    }

    /** Lot C6 : retenue a la source, D compte de la retenue / C 411 du client (lettre avec la facture). */
    void feedWithholding(String withholdingNumber) {
        Map<String, Object> row = jdbc.queryForMap("SELECT w.document_number, w.account, w.amount, w.withheld_on, t.base FROM billing_withholding w "
                + "LEFT JOIN billing_withholding_type t ON t.code = w.type_code WHERE w.withholding_number = ?", withholdingNumber);
        BillingDocument document = documentRepository.findByDocumentNumber((String) row.get("document_number")).orElseThrow();
        LocalDate date = row.get("withheld_on") instanceof java.sql.Date day ? day.toLocalDate() : LocalDate.now();
        Conversion fx = conversion(document.getCurrency(), document.getExchangeRate(), date);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("documentNumber", document.getDocumentNumber());
        data.put("withholdingNumber", withholdingNumber);
        data.put("customerCode", customerCode(document));
        data.put("journalCode", settings.text("accounting.sales.journal", "VT"));
        data.put("clientAccount", settings.text("accounting.sales.client-account", "411"));
        // Decision CD11 : en regime des encaissements, une retenue de TVA solde la TVA en attente (4438) :
        // le client l'a versee a l'Etat, l'exploitant n'a rien a reverser sur cette part.
        data.put("withholdingAccount", paymentRegime() && "TVA".equals(row.get("base"))
                ? settings.text("accounting.vat.pending-account", "4438") : row.get("account"));
        data.put("amount", fx.apply(money((BigDecimal) row.get("amount"))));
        engine.receive("WITHHOLDING_RECORDED", withholdingNumber, "SALES", date, json(data), replay(date));
    }

    /** Lot C6 : retenue annulee apres comptabilisation, contre-passation. */
    void feedWithholdingCancellation(String withholdingNumber) {
        Map<String, Object> row = jdbc.queryForMap("SELECT w.cancelled_at FROM billing_withholding w WHERE w.withholding_number = ?", withholdingNumber);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("withholdingNumber", withholdingNumber);
        data.put("_reverse", Map.of("eventType", "WITHHOLDING_RECORDED", "reference", withholdingNumber, "reason", "Retenue annulee"));
        LocalDate date = date(row.get("cancelled_at") instanceof java.sql.Timestamp at ? at.toInstant() : Instant.now());
        engine.receive("WITHHOLDING_CANCELLED", withholdingNumber, "SALES", date, json(data), replay(date));
    }

    void feedCancellation(String number) {
        BillingDocument document = documentRepository.findByDocumentNumber(number).orElseThrow();
        String original = document.getDocumentType() == BillingDocumentType.CREDIT_NOTE ? CREDIT_NOTE_ISSUED : INVOICE_ISSUED;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("documentNumber", number);
        if (StringUtils.hasText(document.getDepositForDocumentNumber()) && money(document.getPaidAmount()).signum() > 0) {
            // Lot K0 (A4) : facture d'acompte en partie reglee, reste repris par la facture finale. La part
            // reglee est deja deduite (DEPOSIT_APPLIED) : seul le reste non regle quitte le 4191 et le 411.
            // Contre-passer tout le TTC laissait le 4191 debiteur et le 411 crediteur de la part reglee.
            LocalDate day = date(document.getCancelledAt() == null ? Instant.now() : document.getCancelledAt());
            Conversion fx = conversion(document.getCurrency(), document.getExchangeRate(), day);
            data.put("depositFor", document.getDepositForDocumentNumber());
            data.put("customerCode", customerCode(document));
            data.put("journalCode", settings.text("accounting.sales.journal", "VT"));
            data.put("clientAccount", settings.text("accounting.sales.client-account", "411"));
            data.put("depositAccount", settings.text("accounting.sales.deposit-account", "4191"));
            data.put("amount", fx.apply(money(document.getTotalAmount()).subtract(money(document.getPaidAmount())).max(BigDecimal.ZERO)));
            engine.receive(INVOICE_CANCELLED, number, "SALES", day, json(data), replay(day));
            return;
        }
        data.put("_reverse", Map.of("eventType", original, "reference", number, "reason", "Document annule"));
        LocalDate date = date(document.getCancelledAt() == null ? Instant.now() : document.getCancelledAt());
        engine.receive(INVOICE_CANCELLED, number, "SALES", date, json(data), replay(date));
    }

    private ReceivableRiskService receivableRisks;

    /** Lot C14 : ventilation des articles composes (facultatif). */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.sni.bokaticowork.features.billing.service.implementation.QuoteTemplateService bundles;

    /** Lot F19 : facultatif, pour ne pas changer le constructeur. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setReceivableRisks(ReceivableRiskService receivableRisks) {
        this.receivableRisks = receivableRisks;
    }

    /**
     * Lot K1 (A14) : montant deja rembourse sur la transaction d'origine par les remboursements
     * aboutis avant celui-ci (ordre de leur aboutissement).
     */
    private BigDecimal previousRefunds(String originalNumber, PaymentTransaction current) {
        Instant currentAt = current.getPaidAt() != null ? current.getPaidAt() : current.getCreatedAt();
        return transactionRepository.findAllByOriginalTransactionNumber(originalNumber).stream()
                .filter(other -> !other.getId().equals(current.getId()))
                .filter(other -> other.getStatus() == com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus.REFUNDED
                        || other.getStatus() == com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus.REVERSED)
                .filter(other -> {
                    Instant at = other.getPaidAt() != null ? other.getPaidAt() : other.getCreatedAt();
                    return at != null && currentAt != null && (at.isBefore(currentAt) || at.equals(currentAt) && other.getId() < current.getId());
                })
                .map(other -> money(other.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    void feedTransaction(String number) {
        PaymentTransaction transaction = transactionRepository.findByTransactionNumber(number).orElseThrow();
        // Lot K0 (M6) : une relecture ne refait pas le suivi des creances deja fait a la premiere lecture.
        boolean known = !jdbc.queryForList("SELECT 1 FROM acc_posting_event WHERE event_type IN ('PAYMENT_RECEIVED', 'PAYMENT_REFUNDED') "
                + "AND reference = ? AND payload <> '{}'", Integer.class, number).isEmpty();
        String originalNumber = originalTransaction(transaction);
        boolean refund = originalNumber != null;
        PaymentTransaction source = refund ? transactionRepository.findByTransactionNumber(originalNumber).orElse(transaction) : transaction;
        Map<String, Object> data = new LinkedHashMap<>();
        LocalDate date = date(transaction.getPaidAt() != null ? transaction.getPaidAt() : transaction.getCreatedAt());
        // Paiement en devise : converti au taux du jour du paiement ; l'ecart avec le taux de la
        // facture est solde au lettrage (perte ou gain de change).
        Conversion fx = conversion(transaction.getCurrency(), null, date);
        BigDecimal foreignAmount = money(transaction.getAmount());
        data.put("transactionNumber", number);
        data.put("method", transaction.getPaymentMethod().name());
        data.put("clientAccount", settings.text("accounting.sales.client-account", "411"));
        data.put("unallocatedAccount", settings.text("accounting.sales.unallocated-account", "4719"));
        Treasury treasury = treasury(transaction, source);
        data.put("treasuryAccount", treasury.account());
        data.put("journalCode", treasury.journal());
        data.put("treasuryThirdParty", treasury.thirdParty());
        String customer = transaction.getPaymentIntent() == null ? null : transaction.getPaymentIntent().getCustomerCode();
        List<Map<String, Object>> allocations = new ArrayList<>();
        List<Map<String, Object>> vatTransfers = new ArrayList<>();
        BigDecimal left = foreignAmount;
        if (refund) {
            // Lot K1 (A14) : un remboursement prend d'abord la part non affectee de la transaction
            // d'origine (4719 : trop-percu, somme en attente), puis rouvre ses affectations, sans
            // reprendre ce que les remboursements precedents ont deja pris. Meme regle que la
            // facturation (RefundSplit). Avant : les affectations etaient rouvertes d'abord, et deux
            // remboursements partiels rouvraient deux fois la meme.
            List<com.sni.bokaticowork.features.payment.service.support.RefundSplit.Part> parts = allocationRepository
                    .findAllByPaymentTransactionId(source.getId()).stream()
                    .map(allocation -> new com.sni.bokaticowork.features.payment.service.support.RefundSplit.Part(
                            allocation.getBillingDocumentNumber(), money(allocation.getAllocatedAmount())))
                    .toList();
            var split = com.sni.bokaticowork.features.payment.service.support.RefundSplit.split(money(source.getAmount()), parts,
                    previousRefunds(originalNumber, transaction), foreignAmount);
            BigDecimal amount = BigDecimal.ZERO;
            for (var part : split.reopened()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("documentNumber", part.documentNumber());
                item.put("customerCode", documentRepository.findByDocumentNumber(part.documentNumber()).map(this::customerCode).orElse(customer));
                BigDecimal converted = fx.apply(part.amount());
                item.put("amount", converted);
                String riskAccount = receivableRisks == null ? null : receivableRisks.paymentAccount(part.documentNumber());
                if (riskAccount != null) {
                    item.put("clientAccount", riskAccount);
                }
                if (fx.foreign()) {
                    item.put("foreignAmount", part.amount());
                }
                allocations.add(item);
                if (paymentRegime()) {
                    vatTransfers.addAll(vatTransfers(part.documentNumber(), part.amount(), true));
                }
                amount = amount.add(converted);
            }
            left = split.unallocated();
            BigDecimal unallocatedPart = fx.apply(left);
            data.put("amount", amount.add(unallocatedPart));
            data.put("allocations", allocations);
            data.put("unallocated", unallocatedPart);
            data.put("customerCode", customer);
            data.put("vatTransfers", vatTransfers);
            data.put("vatPendingAccount", settings.text("accounting.vat.pending-account", "4438"));
            if (fx.foreign()) {
                data.put("currency", fx.currency());
                data.put("exchangeRate", fx.rate());
                data.put("foreignAmount", foreignAmount);
            }
            engine.receive(PAYMENT_REFUNDED, number, "PAYMENT", date, json(data), replay(date));
            return;
        }
        BigDecimal amount = BigDecimal.ZERO;
        for (PaymentAllocation allocation : allocationRepository.findAllByPaymentTransactionId(source.getId())) {
            if (left.signum() <= 0) {
                break;
            }
            if ("LATE".equals(allocation.getAllocationMode())) {
                // Lot C9 : affectee apres coup, elle reste en attente ici et se solde par sa propre ecriture.
                continue;
            }
            BigDecimal value = money(allocation.getAllocatedAmount()).min(left);
            if (value.signum() <= 0) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("documentNumber", allocation.getBillingDocumentNumber());
            item.put("customerCode", documentRepository.findByDocumentNumber(allocation.getBillingDocumentNumber()).map(this::customerCode).orElse(customer));
            BigDecimal converted = fx.apply(value);
            item.put("amount", converted);
            // Lot F19 : facture douteuse ou litigieuse, credit 4162 ou 4161 ; facture passee en perte, compte de recuperation.
            String riskAccount = receivableRisks == null ? null : receivableRisks.paymentAccount(allocation.getBillingDocumentNumber());
            if (riskAccount != null) {
                item.put("clientAccount", riskAccount);
            }
            if (fx.foreign()) {
                item.put("foreignAmount", value);
            }
            allocations.add(item);
            if (paymentRegime()) {
                vatTransfers.addAll(vatTransfers(allocation.getBillingDocumentNumber(), value, refund));
            }
            amount = amount.add(converted);
            left = left.subtract(value);
        }
        BigDecimal unallocated = fx.apply(left);
        amount = amount.add(unallocated);
        data.put("amount", amount);
        data.put("allocations", allocations);
        data.put("unallocated", unallocated);
        data.put("customerCode", customer);
        data.put("vatTransfers", vatTransfers);
        data.put("vatPendingAccount", settings.text("accounting.vat.pending-account", "4438"));
        if (fx.foreign()) {
            data.put("currency", fx.currency());
            data.put("exchangeRate", fx.rate());
            data.put("foreignAmount", foreignAmount);
        }
        engine.receive(refund ? PAYMENT_REFUNDED : PAYMENT_RECEIVED, number, "PAYMENT", date, json(data), replay(date));
        if (!refund && !known && receivableRisks != null) {
            // Reste du suivi et reprise de la depreciation au prorata de ce qui est encaisse.
            for (Map<String, Object> item : allocations) {
                receivableRisks.onPayment((String) item.get("documentNumber"), (BigDecimal) item.get("amount"), date);
            }
        }
    }

    void feedWalletEntry(String number) {
        WalletLedgerEntry entry = ledgerRepository.findByEntryNumber(number).orElseThrow();
        boolean credit = entry.getDirection() == WalletEntryDirection.CREDIT;
        LocalDate date = date(entry.getCreatedAt());
        Conversion fx = conversion(entry.getWallet().getCurrency(), null, date);
        BigDecimal amount = fx.apply(money(entry.getAmount()));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("entryNumber", number);
        data.put("entryType", entry.getEntryType().name());
        data.put("walletNumber", entry.getWallet().getWalletNumber());
        data.put("ownerCode", entry.getWallet().getOwnerCode());
        data.put("signedAmount", credit ? amount : amount.negate());
        data.put("journalCode", settings.text("accounting.wallet.journal", "PF"));
        String counter;
        String counterThirdParty = null;
        String counterReference = entry.getSourceCode();
        WalletEntryType type = entry.getEntryType();
        switch (type) {
            case CASHBACK, PROMOTIONAL_CREDIT -> counter = settings.text("accounting.wallet.promotion-account", "658");
            case TRANSFER_FEE -> counter = settings.text("accounting.wallet.fee-account", "706");
            case ADMIN_TOPUP, ADMIN_DEBIT, ADJUSTMENT, REVERSAL -> counter = settings.text("accounting.wallet.admin-account", "471");
            case REFUND -> {
                if ("CREDIT_NOTE".equals(entry.getSourceType())) {
                    // Avoir rembourse sur le portefeuille : le client est solde, le portefeuille credite.
                    counter = settings.text("accounting.sales.client-account", "411");
                    BillingDocument note = documentRepository.findByDocumentNumber(entry.getSourceCode()).orElse(null);
                    counterThirdParty = note == null ? entry.getWallet().getOwnerCode() : customerCode(note);
                    counterReference = note != null && StringUtils.hasText(note.getOriginalDocumentNumber()) ? note.getOriginalDocumentNumber()
                            : entry.getSourceCode();
                } else {
                    counter = settings.text("accounting.sales.unallocated-account", "4719");
                }
            }
            default -> counter = settings.text("accounting.sales.unallocated-account", "4719");
        }
        if (com.sni.bokaticowork.features.billing.service.implementation.SecurityDepositService.WALLET_SOURCE.equals(entry.getSourceType())) {
            // Decision CD6 : la caution d'abonnement passe du 165 au portefeuille du client.
            counter = settings.text("accounting.sales.security-deposit-account", "165");
            // Lot K0 (A2) : le 165 est un compte de tiers, la ligne porte le client (avant : sans tiers, piece en attente).
            counterThirdParty = entry.getWallet().getOwnerCode();
        }
        data.put("counterAccount", counter);
        data.put("counterThirdParty", counterThirdParty);
        data.put("counterReference", counterReference);
        if (fx.foreign()) {
            data.put("currency", fx.currency());
            data.put("exchangeRate", fx.rate());
            data.put("foreignAmount", money(entry.getAmount()));
        }
        engine.receive(WALLET_ENTRY, number, "WALLET", date, json(data), replay(date));
    }

    // ------------------------------------------------------------------ tresorerie

    record Treasury(String account, String journal, String thirdParty) {
    }

    private Treasury treasury(PaymentTransaction transaction, PaymentTransaction source) {
        PaymentMethod method = transaction.getPaymentMethod();
        if (method == PaymentMethod.CASH) {
            String session = StringUtils.hasText(transaction.getCashSessionNumber()) ? transaction.getCashSessionNumber() : source.getCashSessionNumber();
            Optional<CashSession> found = StringUtils.hasText(session) ? sessionRepository.findBySessionNumber(session) : Optional.empty();
            if (found.isPresent()) {
                CashAccountingService.RegisterBooks books = cash.ensureBooks(found.get().getCashRegister());
                return new Treasury(books.treasuryAccount(), books.journalCode(), null);
            }
            // Lot K0 (A9) : sans session retrouvee, les especes partaient en 4719 « encaissements a
            // identifier » sans alerte. Elles vont au compte de caisse regle, sinon le fait passe en echec.
            String account = settings.text("accounting.payment.CASH.account", "");
            if (!StringUtils.hasText(account)) {
                throw new IllegalStateException("Paiement en espèces " + transaction.getTransactionNumber()
                        + " sans session de caisse retrouvée : rattachez-le à une session, ou indiquez le compte de caisse par défaut "
                        + "(réglage accounting.payment.CASH.account), puis régénérez l'événement");
            }
            return new Treasury(account, settings.text("accounting.payment.CASH.journal", "ENC"), null);
        }
        Map<PaymentMethod, String[]> defaults = Map.of(
                PaymentMethod.MOBILE_MONEY, new String[]{"5212", "MM-5212"},
                PaymentMethod.MOBILE_MONEY_OFFLINE, new String[]{"5213", "MM-5213"},
                PaymentMethod.BANK_TRANSFER, new String[]{"5211", "BQ-5211"},
                PaymentMethod.CARD, new String[]{"5851", "ENC"},
                PaymentMethod.CHEQUE, new String[]{"513", "ENC"},
                PaymentMethod.WALLET, new String[]{"4719", "PF"},
                PaymentMethod.CREDIT_NOTE, new String[]{"411", "ENC"},
                PaymentMethod.MANUAL_ADJUSTMENT, new String[]{"471", "ENC"});
        String[] fallback = defaults.getOrDefault(method, new String[]{"4719", "ENC"});
        String account = settings.text("accounting.payment." + method.name() + ".account", fallback[0]);
        String journal = settings.text("accounting.payment." + method.name() + ".journal", fallback[1]);
        String thirdParty = chart.find(account).map(found -> found.getThirdPartyType() != null).orElse(false)
                && transaction.getPaymentIntent() != null ? transaction.getPaymentIntent().getCustomerCode() : null;
        return new Treasury(account, journal, thirdParty);
    }

    private String originalTransaction(PaymentTransaction transaction) {
        if (!StringUtils.hasText(transaction.getMetadataJson()) || !transaction.getMetadataJson().contains("originalTransactionNumber")) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(transaction.getMetadataJson());
            String value = node.path("originalTransactionNumber").asText(null);
            return StringUtils.hasText(value) ? value : null;
        } catch (Exception exception) {
            return null;
        }
    }

    /** Compte de produit d'une ligne : reglage par type de ligne. */
    private com.sni.bokaticowork.features.billing.repository.ServiceCatalogItemRepository catalogItems;

    /** Lot B10 : facultatif, pour ne pas changer le constructeur. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setCatalogItems(com.sni.bokaticowork.features.billing.repository.ServiceCatalogItemRepository catalogItems) {
        this.catalogItems = catalogItems;
    }

    public String productAccount(BillingDocumentLine line) {
        // Lot C7 : une caution n'est pas un produit, elle part au 165.
        if (com.sni.bokaticowork.features.billing.service.implementation.SecurityDepositService.isSecurityDepositLine(line,
                line.getDocument(), catalogItems)) {
            return settings.text("accounting.sales.security-deposit-account", "165");
        }
        // Lot B10 : le compte de produit propre a l'article du catalogue passe avant celui du type de ligne.
        if (catalogItems != null && StringUtils.hasText(line.getItemCode())) {
            java.util.Optional<String> own = catalogItems.findByItemCode(line.getItemCode().trim())
                    .map(com.sni.bokaticowork.features.billing.model.ServiceCatalogItem::getRevenueAccount).filter(StringUtils::hasText);
            if (own.isPresent()) {
                return own.get();
            }
        }
        String type = line.getLineType() == null ? "SERVICE" : line.getLineType().name();
        if ("DISBURSEMENT".equals(type)) {
            // Lot C10 : debours, compte de tiers (decision CD9), ni produit ni TVA.
            // Lot K0 (A3) : 4711, et non plus 4718 qui est le compte des ecarts d'equilibrage a regulariser.
            return settings.text("accounting.sales.disbursement-account", "4711");
        }
        String fallback = switch (type) {
            case "PRODUCT" -> "701";
            case "SUBSCRIPTION", "PASS" -> "7062";
            case "BOOKING" -> "7063";
            case "WALLET" -> "4719";
            default -> "706";
        };
        return settings.text("accounting.sales.account." + type, fallback);
    }

    private String customerCode(BillingDocument document) {
        return StringUtils.hasText(document.getCustomerCode()) ? document.getCustomerCode() : document.getCompanyCustomerCode();
    }

    // ------------------------------------------------------------------ lettrage

    /**
     * Lettrage automatique des comptes clients ({@code 411}) et des avances ({@code 4191}) par
     * reference, groupe par groupe, chacun dans sa transaction.
     */
    public PostingDtos.BatchResult autoLetter() {
        int processed = 0;
        int ok = 0;
        List<String> errors = new ArrayList<>();
        // Lot F13 : les fournisseurs (401) se lettrent aussi par reference (numero de facture fournisseur interne).
        // Lot F19 : les creances litigieuses et douteuses (4161, 4162) se lettrent aussi par numero de facture.
        for (String account : List.of(settings.text("accounting.sales.client-account", "411"), "4191", "421",
                settings.text("accounting.purchase.supplier-account", "401"), "4161", "4162")) {
            if (chart.find(account).isEmpty()) {
                continue;
            }
            Set<List<String>> groups = new LinkedHashSet<>();
            Map<List<String>, Set<AccountSide>> sides = new LinkedHashMap<>();
            for (AccountingResponses.OpenLine line : lettering.openLines(account, null)) {
                if (line.reference() == null) {
                    continue;
                }
                List<String> key = java.util.Arrays.asList(line.thirdPartyCode(), line.reference());
                sides.computeIfAbsent(key, ignored -> EnumSet.noneOf(AccountSide.class)).add(line.side());
            }
            sides.forEach((key, set) -> {
                if (set.size() == 2) {
                    groups.add(key);
                }
            });
            for (List<String> key : groups) {
                processed++;
                try {
                    newTransaction.executeWithoutResult(status -> lettering.autoLetterByReference(account, key.get(0), key.get(1)));
                    ok++;
                } catch (RuntimeException exception) {
                    errors.add(account + " " + key + " : " + PostingEngine.readable(exception));
                }
            }
        }
        return new PostingDtos.BatchResult(processed, ok, processed - ok, errors);
    }

    // ------------------------------------------------------------------ controles

    public record InvoiceGap(String documentNumber, String customerCode, BigDecimal balanceDue, BigDecimal accountingOpen, BigDecimal difference) {
    }

    public record Checks(BigDecimal walletTotal, BigDecimal account4191Balance, BigDecimal walletDifference,
                         long invoicesNotPosted, long paymentsNotPosted, long walletEntriesNotPosted,
                         List<InvoiceGap> invoiceGaps) {
    }

    /**
     * Controles de coherence : {@code 4191} face au total des portefeuilles ; reste du par facture
     * face au reste ouvert en {@code 411} ; faits pas encore comptabilises.
     */
    public Checks checks(int limit) {
        BigDecimal walletTotal = Optional.ofNullable(jdbc.queryForObject(
                "SELECT COALESCE(SUM(ledger_balance), 0) FROM wallet_account", BigDecimal.class)).orElse(BigDecimal.ZERO);
        BigDecimal balance4191 = Optional.ofNullable(lineRepository.balanceOf("4191", List.of(EntryStatus.VALIDATED, EntryStatus.REVERSED)))
                .orElse(BigDecimal.ZERO).negate();
        long invoices = count("SELECT COUNT(*) FROM billing_document d WHERE d.document_type IN ('INVOICE', 'CORRECTIVE_INVOICE', 'DEBIT_NOTE', 'CREDIT_NOTE') "
                + "AND d.issued_at IS NOT NULL AND d.deleted_at IS NULL AND NOT EXISTS (SELECT 1 FROM acc_posting_event e WHERE e.reference = d.document_number "
                + "AND e.event_type IN ('INVOICE_ISSUED', 'CREDIT_NOTE_ISSUED') AND e.status = 'POSTED')");
        long payments = count("SELECT COUNT(*) FROM payment_transaction t WHERE t.status IN ('SUCCEEDED', 'REFUNDED', 'REVERSED') AND NOT EXISTS "
                + "(SELECT 1 FROM acc_posting_event e WHERE e.reference = t.transaction_number AND e.event_type IN ('PAYMENT_RECEIVED', 'PAYMENT_REFUNDED') AND e.status = 'POSTED')");
        long wallet = count("SELECT COUNT(*) FROM wallet_ledger_entry w WHERE w.entry_type NOT IN ('HOLD', 'HOLD_RELEASE') AND NOT EXISTS "
                + "(SELECT 1 FROM acc_posting_event e WHERE e.reference = w.entry_number AND e.event_type = 'WALLET_ENTRY' AND e.status = 'POSTED')");
        String client = settings.text("accounting.sales.client-account", "411");
        Map<String, BigDecimal> open = new LinkedHashMap<>();
        if (chart.find(client).isPresent()) {
            for (AccountingResponses.OpenLine line : lettering.openLines(client, null)) {
                if (line.reference() != null) {
                    open.merge(line.reference(), line.side() == AccountSide.DEBIT ? line.remaining() : line.remaining().negate(), BigDecimal::add);
                }
            }
        }
        List<InvoiceGap> gaps = new ArrayList<>();
        for (Map<String, Object> row : jdbc.queryForList("SELECT d.document_number, COALESCE(d.customer_code, d.company_customer_code) AS customer, d.balance_due "
                + "FROM billing_document d WHERE d.document_type IN ('INVOICE', 'CORRECTIVE_INVOICE', 'DEBIT_NOTE') AND d.issued_at IS NOT NULL "
                + "AND d.deleted_at IS NULL AND d.status NOT IN ('CANCELLED', 'VOIDED', 'WRITTEN_OFF') AND EXISTS (SELECT 1 FROM acc_posting_event e "
                + "WHERE e.reference = d.document_number AND e.event_type = 'INVOICE_ISSUED' AND e.status = 'POSTED')")) {
            String number = (String) row.get("document_number");
            BigDecimal due = money((BigDecimal) row.get("balance_due"));
            BigDecimal accounting = money(open.getOrDefault(number, BigDecimal.ZERO));
            if (due.compareTo(accounting) != 0) {
                gaps.add(new InvoiceGap(number, (String) row.get("customer"), due, accounting, due.subtract(accounting)));
                if (gaps.size() >= limit) {
                    break;
                }
            }
        }
        return new Checks(money(walletTotal), money(balance4191), money(walletTotal.subtract(balance4191)), invoices, payments, wallet, gaps);
    }

    private long count(String sql) {
        Long value = jdbc.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }

    // ------------------------------------------------------------------ fiscalite (lot F11)

    private boolean paymentRegime() {
        return "PAYMENT".equalsIgnoreCase(settings.text("accounting.vat.chargeability", "INVOICE"));
    }

    /** Code de taxe : TVA et son taux (TVA18), EXO pour un client exonere, rien sans TVA. */
    private String vatCode(BillingDocument document, BigDecimal vat) {
        if (vat.signum() == 0) {
            return StringUtils.hasText(document.getCustomerVatExemptionNote()) ? "EXO" : null;
        }
        return documentLineRepository.findAllByDocumentOrderByLineOrderAscIdAsc(document).stream()
                .map(BillingDocumentLine::getVatRate).filter(rate -> rate != null && rate.signum() > 0).findFirst()
                .map(rate -> "TVA" + (rate.compareTo(BigDecimal.ONE) < 0 ? rate.movePointRight(2) : rate).stripTrailingZeros().toPlainString())
                .orElse("TVA");
    }

    /** TVA encore en attente d'exigibilite pour une facture. */
    private BigDecimal pendingRemaining(String documentNumber) {
        BigDecimal value = jdbc.queryForObject("SELECT COALESCE(SUM(l.credit - l.debit), 0) FROM acc_entry_line l JOIN acc_entry e ON e.id = l.entry_id "
                + "WHERE l.account_number = ? AND l.reference = ? AND e.status IN ('VALIDATED', 'REVERSED')", BigDecimal.class,
                settings.text("accounting.vat.pending-account", "4438"), documentNumber);
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * Part de la TVA en attente d'une facture qui devient exigible avec un reglement (ou qui revient
     * en attente avec un remboursement), au prorata du montant regle sur le total de la facture ; le
     * dernier reglement prend le reste.
     */
    private List<Map<String, Object>> vatTransfers(String documentNumber, BigDecimal paid, boolean refund) {
        BillingDocument document = documentRepository.findByDocumentNumber(documentNumber).orElse(null);
        if (document == null || document.getDocumentType() == BillingDocumentType.CREDIT_NOTE || money(document.getTotalAmount()).signum() == 0) {
            return List.of();
        }
        List<String> status = jdbc.queryForList("SELECT status FROM acc_posting_event WHERE event_type = ? AND reference = ?", String.class,
                INVOICE_ISSUED, documentNumber);
        if (status.isEmpty() || !"POSTED".equals(status.get(0))) {
            throw new IllegalStateException("La facture " + documentNumber + " n'est pas encore comptabilisee : la TVA de l'encaissement attend");
        }
        BigDecimal total = money(document.getTotalAmount());
        String pending = settings.text("accounting.vat.pending-account", "4438");
        String extraCode = settings.text("accounting.vat.extra-tax-code", "CA");
        List<Map<String, Object>> result = new ArrayList<>();
        jdbc.query("SELECT l.tax_code, COALESCE(SUM(CASE WHEN e.source_type = ? THEN l.credit ELSE 0 END), 0) AS total, "
                + "COALESCE(SUM(CASE WHEN e.source_type = ? THEN l.tax_base ELSE 0 END), 0) AS base, COALESCE(SUM(l.credit - l.debit), 0) AS remaining "
                + "FROM acc_entry_line l JOIN acc_entry e ON e.id = l.entry_id WHERE l.account_number = ? AND l.reference = ? "
                + "AND e.status IN ('VALIDATED', 'REVERSED') GROUP BY l.tax_code", rs -> {
            BigDecimal invoiceTax = rs.getBigDecimal("total");
            BigDecimal remaining = rs.getBigDecimal("remaining");
            BigDecimal share = invoiceTax.multiply(paid).divide(total, 0, RoundingMode.HALF_UP);
            if (refund) {
                share = share.min(invoiceTax.subtract(remaining));
            } else if (share.compareTo(remaining) > 0 || remaining.subtract(share).abs().compareTo(BigDecimal.ONE) <= 0) {
                share = remaining;
            }
            if (share.signum() <= 0) {
                return;
            }
            String code = rs.getString("tax_code");
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("documentNumber", documentNumber);
            item.put("taxCode", code);
            item.put("vat", money(share));
            item.put("base", money(rs.getBigDecimal("base").multiply(paid).divide(total, 0, RoundingMode.HALF_UP)));
            item.put("account", extraCode.equals(code) ? settings.text("accounting.sales.extra-tax-account", "4431")
                    : settings.text("accounting.sales.vat-account", "4431"));
            result.add(item);
        }, INVOICE_ISSUED, INVOICE_ISSUED, pending, documentNumber);
        return result;
    }

    // ------------------------------------------------------------------ outils

    /** Conversion d'une devise en XAF ; neutre pour le XAF. */
    record Conversion(String currency, BigDecimal rate) {
        boolean foreign() {
            return rate != null;
        }

        BigDecimal apply(BigDecimal value) {
            return rate == null ? value : value.multiply(rate).setScale(0, RoundingMode.HALF_UP).setScale(4, RoundingMode.HALF_UP);
        }
    }

    private Conversion conversion(String currency, BigDecimal documentRate, LocalDate date) {
        String code = StringUtils.hasText(currency) ? currency.trim().toUpperCase(java.util.Locale.ROOT) : ExchangeRateService.BASE;
        if (ExchangeRateService.BASE.equals(code)) {
            return new Conversion(code, null);
        }
        if (documentRate != null && documentRate.signum() > 0) {
            return new Conversion(code, documentRate);
        }
        BigDecimal rate = rates.rate(code, date).orElseThrow(() -> new com.sni.bokaticowork.core.exception.customs.BadRequestException(
                "Aucun taux " + code + " au " + date + " : saisissez-le ou recuperez les taux du jour"));
        return new Conversion(code, rate);
    }

    private boolean replay(LocalDate date) {
        LocalDate until = settings.replayUntil();
        return until != null && date != null && date.isBefore(until);
    }

    private static LocalDate date(Instant at) {
        return (at == null ? Instant.now() : at).atZone(PostingEngine.AccountingClock.ZONE).toLocalDate();
    }

    private String json(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception exception) {
            throw new IllegalStateException("Donnees comptables illisibles : " + exception.getMessage(), exception);
        }
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(4, RoundingMode.HALF_UP);
    }

    static String references(List<String> values) {
        return values.stream().collect(Collectors.joining(", "));
    }
}
