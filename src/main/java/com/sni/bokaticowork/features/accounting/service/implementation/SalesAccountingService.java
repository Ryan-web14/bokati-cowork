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
public class SalesAccountingService {

    public static final String INVOICE_ISSUED = "INVOICE_ISSUED";
    public static final String CREDIT_NOTE_ISSUED = "CREDIT_NOTE_ISSUED";
    public static final String INVOICE_CANCELLED = "INVOICE_CANCELLED";
    public static final String PAYMENT_RECEIVED = "PAYMENT_RECEIVED";
    public static final String PAYMENT_REFUNDED = "PAYMENT_REFUNDED";
    public static final String WALLET_ENTRY = "WALLET_ENTRY";

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

    public SalesAccountingService(BillingDocumentRepository documentRepository, BillingDocumentLineRepository documentLineRepository,
                                  PaymentTransactionRepository transactionRepository, PaymentAllocationRepository allocationRepository,
                                  WalletLedgerEntryRepository ledgerRepository, CashSessionRepository sessionRepository,
                                  AccEntryLineRepository lineRepository, CashAccountingService cash, AccountingLetteringService lettering,
                                  AccountingChartService chart, AccountingSettings settings, @Lazy PostingEngine engine, ObjectMapper objectMapper,
                                  DataSource dataSource, PlatformTransactionManager transactionManager) {
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
                    .forEach(number -> counter.run(number, () -> feedDocument(number)));
            jdbc.queryForList("SELECT d.document_number FROM billing_document d WHERE d.status IN ('CANCELLED', 'VOIDED') AND d.document_number IS NOT NULL "
                    + "AND EXISTS (SELECT 1 FROM acc_posting_event e WHERE e.event_type IN ('INVOICE_ISSUED', 'CREDIT_NOTE_ISSUED') AND e.reference = d.document_number) "
                    + "AND NOT EXISTS (SELECT 1 FROM acc_posting_event e WHERE e.event_type = 'INVOICE_CANCELLED' AND e.reference = d.document_number) "
                    + "ORDER BY d.cancelled_at LIMIT ?", String.class, batch)
                    .forEach(number -> counter.run(number, () -> feedCancellation(number)));
        }
        if (settings.familyEnabled("PAYMENT")) {
            Instant settled = Instant.now().minusSeconds(60L * Math.max(0, settings.paymentDelayMinutes()));
            jdbc.queryForList("SELECT t.transaction_number FROM payment_transaction t WHERE t.status IN ('SUCCEEDED', 'REFUNDED', 'REVERSED') "
                    + "AND COALESCE(t.paid_at, t.created_at) <= ? AND NOT EXISTS (SELECT 1 FROM acc_posting_event e "
                    + "WHERE e.event_type IN ('PAYMENT_RECEIVED', 'PAYMENT_REFUNDED') AND e.reference = t.transaction_number) "
                    + "ORDER BY COALESCE(t.paid_at, t.created_at) LIMIT ?", String.class, java.sql.Timestamp.from(settled), batch)
                    .forEach(number -> counter.run(number, () -> feedTransaction(number)));
        }
        if (settings.familyEnabled("WALLET")) {
            jdbc.queryForList("SELECT w.entry_number FROM wallet_ledger_entry w WHERE w.entry_type NOT IN ('HOLD', 'HOLD_RELEASE') "
                    + "AND NOT EXISTS (SELECT 1 FROM acc_posting_event e WHERE e.event_type = 'WALLET_ENTRY' AND e.reference = w.entry_number) "
                    + "ORDER BY w.created_at LIMIT ?", String.class, batch)
                    .forEach(number -> counter.run(number, () -> feedWalletEntry(number)));
        }
        return counter.result();
    }

    private final class Counter {
        int processed;
        int ok;
        final List<String> errors = new ArrayList<>();

        void run(String reference, Runnable action) {
            processed++;
            try {
                newTransaction.executeWithoutResult(status -> action.run());
                ok++;
            } catch (RuntimeException exception) {
                errors.add(reference + " : " + PostingEngine.readable(exception));
            }
        }

        PostingDtos.BatchResult result() {
            return new PostingDtos.BatchResult(processed, ok, processed - ok, errors);
        }
    }

    void feedDocument(String number) {
        BillingDocument document = documentRepository.findByDocumentNumber(number).orElseThrow();
        boolean creditNote = document.getDocumentType() == BillingDocumentType.CREDIT_NOTE;
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
        BigDecimal taxable = money(document.getTaxableAmount());
        BigDecimal vat = money(document.getVatAmount());
        BigDecimal extra = money(document.getTaxAmount()).subtract(vat).max(BigDecimal.ZERO);
        List<Map<String, Object>> lines = new ArrayList<>();
        BigDecimal gross = BigDecimal.ZERO;
        for (BillingDocumentLine line : documentLineRepository.findAllByDocumentOrderByLineOrderAscIdAsc(document)) {
            BigDecimal amount = money(line.getSubtotalAmount() != null ? line.getSubtotalAmount() : line.getTaxableAmount());
            if (amount.signum() == 0 || Boolean.TRUE.equals(line.getOptional())) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("account", productAccount(line));
            item.put("amount", amount);
            item.put("label", line.getDescription());
            lines.add(item);
            gross = gross.add(amount);
        }
        data.put("lines", lines);
        // Remise : ecart entre le brut des lignes et la base taxable (remises de lignes et de document).
        data.put("reduction", gross.subtract(taxable));
        data.put("vatAmount", vat);
        data.put("extraTaxAmount", extra);
        data.put("totalAmount", money(document.getTotalAmount()));
        data.put("dueDate", document.getDueDate() == null ? null : document.getDueDate().toString());
        data.put("letteringReference", creditNote && StringUtils.hasText(document.getOriginalDocumentNumber())
                ? document.getOriginalDocumentNumber() : document.getDocumentNumber());
        data.put("originalDocumentNumber", document.getOriginalDocumentNumber());
        LocalDate date = document.getIssueDate() != null ? document.getIssueDate() : date(document.getIssuedAt());
        engine.receive(creditNote ? CREDIT_NOTE_ISSUED : INVOICE_ISSUED, number, "SALES", date, json(data), replay(date));
    }

    void feedCancellation(String number) {
        BillingDocument document = documentRepository.findByDocumentNumber(number).orElseThrow();
        String original = document.getDocumentType() == BillingDocumentType.CREDIT_NOTE ? CREDIT_NOTE_ISSUED : INVOICE_ISSUED;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("documentNumber", number);
        data.put("_reverse", Map.of("eventType", original, "reference", number, "reason", "Document annule"));
        LocalDate date = date(document.getCancelledAt() == null ? Instant.now() : document.getCancelledAt());
        engine.receive(INVOICE_CANCELLED, number, "SALES", date, json(data), replay(date));
    }

    void feedTransaction(String number) {
        PaymentTransaction transaction = transactionRepository.findByTransactionNumber(number).orElseThrow();
        String originalNumber = originalTransaction(transaction);
        boolean refund = originalNumber != null;
        PaymentTransaction source = refund ? transactionRepository.findByTransactionNumber(originalNumber).orElse(transaction) : transaction;
        Map<String, Object> data = new LinkedHashMap<>();
        BigDecimal amount = money(transaction.getAmount());
        data.put("transactionNumber", number);
        data.put("method", transaction.getPaymentMethod().name());
        data.put("amount", amount);
        data.put("clientAccount", settings.text("accounting.sales.client-account", "411"));
        data.put("unallocatedAccount", settings.text("accounting.sales.unallocated-account", "4719"));
        Treasury treasury = treasury(transaction, source);
        data.put("treasuryAccount", treasury.account());
        data.put("journalCode", treasury.journal());
        data.put("treasuryThirdParty", treasury.thirdParty());
        String customer = transaction.getPaymentIntent() == null ? null : transaction.getPaymentIntent().getCustomerCode();
        List<Map<String, Object>> allocations = new ArrayList<>();
        BigDecimal left = amount;
        for (PaymentAllocation allocation : allocationRepository.findAllByPaymentTransactionId(source.getId())) {
            if (left.signum() <= 0) {
                break;
            }
            BigDecimal value = money(allocation.getAllocatedAmount()).min(left);
            if (value.signum() <= 0) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("documentNumber", allocation.getBillingDocumentNumber());
            item.put("customerCode", documentRepository.findByDocumentNumber(allocation.getBillingDocumentNumber()).map(this::customerCode).orElse(customer));
            item.put("amount", value);
            allocations.add(item);
            left = left.subtract(value);
        }
        data.put("allocations", allocations);
        data.put("unallocated", left);
        data.put("customerCode", customer);
        LocalDate date = date(transaction.getPaidAt() != null ? transaction.getPaidAt() : transaction.getCreatedAt());
        engine.receive(refund ? PAYMENT_REFUNDED : PAYMENT_RECEIVED, number, "PAYMENT", date, json(data), replay(date));
    }

    void feedWalletEntry(String number) {
        WalletLedgerEntry entry = ledgerRepository.findByEntryNumber(number).orElseThrow();
        boolean credit = entry.getDirection() == WalletEntryDirection.CREDIT;
        BigDecimal amount = money(entry.getAmount());
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
        data.put("counterAccount", counter);
        data.put("counterThirdParty", counterThirdParty);
        data.put("counterReference", counterReference);
        LocalDate date = date(entry.getCreatedAt());
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
            return new Treasury(settings.text("accounting.payment.CASH.account", "4719"), settings.text("accounting.payment.CASH.journal", "ENC"), null);
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
    private String productAccount(BillingDocumentLine line) {
        String type = line.getLineType() == null ? "SERVICE" : line.getLineType().name();
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
        for (String account : List.of(settings.text("accounting.sales.client-account", "411"), "4191", "421")) {
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

    // ------------------------------------------------------------------ outils

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
