package com.sni.bokaticowork.features.accounting.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.enums.EntryOrigin;
import com.sni.bokaticowork.features.accounting.enums.EntryStatus;
import com.sni.bokaticowork.features.accounting.model.AccEntry;
import com.sni.bokaticowork.features.accounting.model.AccVatReturn;
import com.sni.bokaticowork.features.accounting.repository.AccVatReturnRepository;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.fund.voucher.VoucherService;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Fiscalite (lot F11) : declaration mensuelle de TVA, controle face aux factures, liquidation.
 *
 * <p>TVA collectee : mouvement net des comptes de TVA facturee ({@code accounting.vat.collected-accounts})
 * sur la periode ; TVA deductible : mouvement net des comptes de TVA recuperable
 * ({@code accounting.vat.deductible-accounts}) ; credit reporte : solde du compte de credit de TVA
 * au debut de la periode. La piece de liquidation solde ces comptes contre la TVA due ou le credit
 * a reporter. Les pieces de liquidation ({@code VAT_RETURN}) sont exclues des calculs.
 */
@Service
@Transactional
public class VatService {

    public static final String SOURCE = "VAT_RETURN";
    static final List<String> POSTED = List.of(EntryStatus.VALIDATED.name(), EntryStatus.REVERSED.name());
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final AccVatReturnRepository repository;
    private final AccountingSettings settings;
    private final AccountingEntryService entries;
    private final VoucherService vouchers;
    private final ExchangeRateService rates;
    private final CashActorResolver actors;
    private final JdbcTemplate jdbc;

    public VatService(AccVatReturnRepository repository, AccountingSettings settings, AccountingEntryService entries, VoucherService vouchers,
                      ExchangeRateService rates, CashActorResolver actors, DataSource dataSource) {
        this.repository = repository;
        this.settings = settings;
        this.entries = entries;
        this.vouchers = vouchers;
        this.rates = rates;
        this.actors = actors;
        this.jdbc = new JdbcTemplate(dataSource);
    }

    // ------------------------------------------------------------------ reglages

    public String regime() {
        return "PAYMENT".equalsIgnoreCase(settings.text("accounting.vat.chargeability", "INVOICE")) ? "PAYMENT" : "INVOICE";
    }

    List<String> collectedAccounts() {
        return list(settings.text("accounting.vat.collected-accounts", "4431,4432"));
    }

    List<String> deductibleAccounts() {
        return list(settings.text("accounting.vat.deductible-accounts", "4451,4452,4454"));
    }

    String dueAccount() {
        return settings.text("accounting.vat.due-account", "4441");
    }

    String creditAccount() {
        return settings.text("accounting.vat.credit-account", "4449");
    }

    String pendingAccount() {
        return settings.text("accounting.vat.pending-account", "4438");
    }

    private static List<String> list(String value) {
        return Arrays.stream(value.split(",")).map(String::trim).filter(StringUtils::hasText).distinct().toList();
    }

    // ------------------------------------------------------------------ etat

    public record AccountAmount(String account, String label, BigDecimal amount) {
    }

    public record CodeAmount(String taxCode, BigDecimal base, BigDecimal tax) {
    }

    public record DocumentTax(String documentNumber, String documentType, LocalDate date, String customerName, String currency,
                              BigDecimal taxable, BigDecimal vat, BigDecimal extraTax, BigDecimal taxXaf, BigDecimal accountingTax,
                              BigDecimal difference, String event, boolean sealed, String fiscalNumber) {
    }

    /**
     * {@code sealedTaxTotal} : taxes des seuls documents scelles (valides, numero fiscal) ; la
     * comptabilite enregistre une facture des son emission, un ecart avec {@code invoiceTaxTotal}
     * signale des factures emises mais pas encore scellees.
     */
    public record Detail(List<AccountAmount> collected, List<AccountAmount> deductible, List<CodeAmount> byCode, List<DocumentTax> documents,
                         BigDecimal sealedTaxTotal) {
    }

    @Transactional(readOnly = true)
    public List<AccVatReturn> list() {
        return repository.findTop36ByOrderByPeriodCodeDesc();
    }

    @Transactional(readOnly = true)
    public AccVatReturn get(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Declaration de TVA introuvable"));
    }

    @Transactional(readOnly = true)
    public Detail detail(Long id) {
        AccVatReturn record = get(id);
        return read(record.getDetailJson());
    }

    /** Prepare (ou recalcule) la declaration d'un mois, tant qu'elle n'est pas validee. */
    public AccVatReturn prepare(String period) {
        require("VAT_RETURN:PREPARE");
        YearMonth month = month(period);
        AccVatReturn record = repository.findByPeriodCode(month.toString()).orElseGet(() -> AccVatReturn.builder()
                .periodCode(month.toString()).startDate(month.atDay(1)).endDate(month.atEndOfMonth()).build());
        if (!"DRAFT".equals(record.getStatus())) {
            throw new BadRequestException("La declaration " + record.getPeriodCode() + " est deja " + label(record.getStatus()));
        }
        compute(record);
        record.setPreparedBy(actors.resolve("SYSTEM"));
        record.setPreparedAt(Instant.now());
        return repository.save(record);
    }

    /** Validation par une autre personne que le preparateur ; l'etat est recalcule puis fige. */
    public AccVatReturn validate(Long id) {
        require("VAT_RETURN:VALIDATE");
        AccVatReturn record = get(id);
        if (!"DRAFT".equals(record.getStatus())) {
            throw new BadRequestException("La declaration " + record.getPeriodCode() + " est deja " + label(record.getStatus()));
        }
        String actor = actors.resolve("SYSTEM");
        if (CashActorResolver.samePerson(actor, record.getPreparedBy())) {
            throw new BadRequestException("La declaration se valide par une autre personne que celle qui l'a preparee");
        }
        repository.findByPeriodCode(YearMonth.parse(record.getPeriodCode()).minusMonths(1).toString())
                .filter(previous -> !"LIQUIDATED".equals(previous.getStatus()))
                .ifPresent(previous -> {
                    throw new BadRequestException("La declaration " + previous.getPeriodCode() + " n'est pas liquidee : le credit reporte serait faux");
                });
        compute(record);
        record.setStatus("VALIDATED");
        record.setValidatedBy(actor);
        record.setValidatedAt(Instant.now());
        return repository.save(record);
    }

    /**
     * Liquidation : piece datee du dernier jour du mois qui solde la TVA collectee et deductible du
     * mois contre la TVA due ({@code 4441}) ou le credit a reporter ({@code 4449}).
     */
    public AccVatReturn liquidate(Long id) {
        require("VAT_RETURN:VALIDATE");
        AccVatReturn record = get(id);
        if (!"VALIDATED".equals(record.getStatus())) {
            throw new BadRequestException("Seule une declaration validee se liquide");
        }
        Detail detail = read(record.getDetailJson());
        String journal = settings.text("accounting.vat.journal", "OD");
        String label = "Liquidation TVA " + record.getPeriodCode();
        List<AccountingRequests.EntryLine> lines = new ArrayList<>();
        for (AccountAmount line : detail.collected()) {
            add(lines, line.account(), label, line.amount(), true);
        }
        for (AccountAmount line : detail.deductible()) {
            add(lines, line.account(), label, line.amount(), false);
        }
        add(lines, creditAccount(), "Credit de TVA impute", record.getCreditBroughtForward(), false);
        add(lines, dueAccount(), "TVA due " + record.getPeriodCode(), record.getDueAmount(), false);
        add(lines, creditAccount(), "Credit de TVA a reporter", record.getCreditCarriedForward(), true);
        String actor = actors.resolve("SYSTEM");
        if (!lines.isEmpty()) {
            AccEntry entry = entries.post(journal, record.getEndDate(), label, EntryOrigin.PLATFORM, SOURCE, record.getPeriodCode(), lines, actor);
            if (entry.getStatus() != EntryStatus.VALIDATED) {
                throw new BadRequestException("La piece de liquidation n'a pas pu etre validee : " + entry.getPendingDetail());
            }
            record.setLiquidationEntryNumber(entry.getEntryNumber());
        }
        record.setStatus("LIQUIDATED");
        record.setLiquidatedBy(actor);
        record.setLiquidatedAt(Instant.now());
        return repository.save(record);
    }

    /** Une declaration en brouillon se supprime ; validee ou liquidee, elle reste. */
    public void delete(Long id) {
        require("VAT_RETURN:PREPARE");
        AccVatReturn record = get(id);
        if (!"DRAFT".equals(record.getStatus())) {
            throw new BadRequestException("Seul un brouillon se supprime");
        }
        repository.delete(record);
    }

    /** Debit si {@code debit}, sinon credit ; un montant negatif change de cote. */
    private static void add(List<AccountingRequests.EntryLine> lines, String account, String label, BigDecimal amount, boolean debit) {
        if (amount == null || amount.signum() == 0) {
            return;
        }
        boolean side = amount.signum() > 0 == debit;
        BigDecimal value = amount.abs();
        lines.add(new AccountingRequests.EntryLine(account, null, label, side ? value : null, side ? null : value,
                null, null, null, null, null, null, null, null));
    }

    // ------------------------------------------------------------------ calcul

    void compute(AccVatReturn record) {
        LocalDate from = record.getStartDate();
        LocalDate to = record.getEndDate();
        List<AccountAmount> collected = movements(collectedAccounts(), from, to, true);
        List<AccountAmount> deductible = movements(deductibleAccounts(), from, to, false);
        BigDecimal collectedTotal = sum(collected);
        BigDecimal deductibleTotal = sum(deductible);
        BigDecimal creditIn = balanceBefore(creditAccount(), from).max(BigDecimal.ZERO);
        BigDecimal net = collectedTotal.subtract(deductibleTotal).subtract(creditIn);
        List<DocumentTax> documents = documents(from, to);
        BigDecimal invoiceTax = documents.stream().map(DocumentTax::taxXaf).reduce(BigDecimal.ZERO, BigDecimal::add);
        String regime = regime();
        record.setRegime(regime);
        record.setCollected(collectedTotal);
        record.setDeductible(deductibleTotal);
        record.setCreditBroughtForward(creditIn);
        record.setDueAmount(net.max(BigDecimal.ZERO));
        record.setCreditCarriedForward(net.negate().max(BigDecimal.ZERO));
        record.setRevenue(revenue(from, to));
        record.setInvoiceTaxTotal(invoiceTax);
        record.setControlDifference("INVOICE".equals(regime) ? collectedTotal.subtract(invoiceTax) : null);
        record.setPendingBalance(balanceUpTo(pendingAccount(), to).negate());
        try {
            BigDecimal sealed = documents.stream().filter(DocumentTax::sealed).map(DocumentTax::taxXaf).reduce(BigDecimal.ZERO, BigDecimal::add);
            record.setDetailJson(JSON.writeValueAsString(new Detail(collected, deductible, byCode(from, to), documents, sealed)));
        } catch (Exception exception) {
            throw new IllegalStateException("Detail de la declaration illisible : " + exception.getMessage(), exception);
        }
    }

    /** Mouvement net par compte sur la periode : credit moins debit pour la TVA collectee, l'inverse sinon. */
    private List<AccountAmount> movements(List<String> accounts, LocalDate from, LocalDate to, boolean creditNormal) {
        List<AccountAmount> result = new ArrayList<>();
        for (String account : accounts) {
            BigDecimal value = jdbc.queryForObject("SELECT COALESCE(SUM(l.credit - l.debit), 0) FROM acc_entry_line l JOIN acc_entry e ON e.id = l.entry_id "
                    + "WHERE l.account_number = ? AND e.status IN ('VALIDATED', 'REVERSED') AND e.entry_date BETWEEN ? AND ? "
                    + "AND COALESCE(e.source_type, '') <> ?", BigDecimal.class, account, Date.valueOf(from), Date.valueOf(to), SOURCE);
            BigDecimal amount = money(creditNormal ? value : value.negate());
            if (amount.signum() != 0) {
                result.add(new AccountAmount(account, accountLabel(account), amount));
            }
        }
        return result;
    }

    private List<CodeAmount> byCode(LocalDate from, LocalDate to) {
        List<CodeAmount> result = new ArrayList<>();
        String in = String.join(",", Collections.nCopies(collectedAccounts().size(), "?"));
        List<Object> args = new ArrayList<>(collectedAccounts());
        args.add(Date.valueOf(from));
        args.add(Date.valueOf(to));
        args.add(SOURCE);
        jdbc.query("SELECT COALESCE(l.tax_code, 'SANS CODE') AS code, COALESCE(SUM(CASE WHEN l.credit > 0 THEN l.tax_base ELSE -l.tax_base END), 0) AS base, "
                + "COALESCE(SUM(l.credit - l.debit), 0) AS tax FROM acc_entry_line l JOIN acc_entry e ON e.id = l.entry_id "
                + "WHERE l.account_number IN (" + in + ") AND e.status IN ('VALIDATED', 'REVERSED') AND e.entry_date BETWEEN ? AND ? "
                + "AND COALESCE(e.source_type, '') <> ? GROUP BY COALESCE(l.tax_code, 'SANS CODE') ORDER BY 1", rs -> {
            result.add(new CodeAmount(rs.getString("code"), money(rs.getBigDecimal("base")), money(rs.getBigDecimal("tax"))));
        }, args.toArray());
        return result;
    }

    /**
     * Taxes des factures et avoirs emis dans le mois (avoirs en negatif), et des documents annules
     * dans le mois (en negatif), convertis en XAF comme en comptabilite ; a cote, la taxe portee par
     * leurs pieces comptables.
     */
    private List<DocumentTax> documents(LocalDate from, LocalDate to) {
        List<DocumentTax> result = new ArrayList<>();
        String issueDate = "COALESCE(d.issue_date, (d.issued_at AT TIME ZONE 'Africa/Brazzaville')::date)";
        String cancelDate = "(d.cancelled_at AT TIME ZONE 'Africa/Brazzaville')::date";
        String base = "SELECT d.document_number, d.document_type, d.customer_name, d.currency, d.exchange_rate, d.taxable_amount, d.vat_amount, "
                + "d.tax_amount, d.locked, d.fiscal_number, %s AS fact_date, '%s' AS event, " + issueDate + " AS issue_day FROM billing_document d WHERE d.document_type IN ('INVOICE', 'CORRECTIVE_INVOICE', "
                + "'DEBIT_NOTE', 'CREDIT_NOTE') AND d.issued_at IS NOT NULL AND d.deleted_at IS NULL AND d.document_number IS NOT NULL AND %s BETWEEN ? AND ?";
        String sql = String.format(base, issueDate, "EMISSION", issueDate) + " UNION ALL "
                + String.format(base, cancelDate, "ANNULATION", cancelDate) + " AND d.status IN ('CANCELLED', 'VOIDED') ORDER BY 11, 1";
        jdbc.query(sql, rs -> {
            String number = rs.getString("document_number");
            String type = rs.getString("document_type");
            String event = rs.getString("event");
            LocalDate date = rs.getDate("fact_date").toLocalDate();
            BigDecimal vat = money(rs.getBigDecimal("vat_amount"));
            BigDecimal extra = money(rs.getBigDecimal("tax_amount")).subtract(vat).max(BigDecimal.ZERO);
            String currency = rs.getString("currency");
            BigDecimal rate = rate(currency, rs.getBigDecimal("exchange_rate"), rs.getDate("issue_day").toLocalDate(), number);
            BigDecimal tax = rate == null ? vat.add(extra) : xaf(vat, rate).add(xaf(extra, rate));
            int sign = ("CREDIT_NOTE".equals(type) ? -1 : 1) * ("ANNULATION".equals(event) ? -1 : 1);
            BigDecimal taxXaf = sign > 0 ? tax : tax.negate();
            BigDecimal accounting = accountingTax(number, "ANNULATION".equals(event));
            result.add(new DocumentTax(number, type, date, rs.getString("customer_name"), currency, money(rs.getBigDecimal("taxable_amount")),
                    vat, extra, taxXaf, accounting, accounting == null ? null : accounting.subtract(taxXaf), event, rs.getBoolean("locked"),
                    rs.getString("fiscal_number")));
        }, Date.valueOf(from), Date.valueOf(to), Date.valueOf(from), Date.valueOf(to));
        return result;
    }

    /** Taxe portee par les pieces du document (TVA facturee, en attente comprise) ; nul si pas encore comptabilise. */
    private BigDecimal accountingTax(String documentNumber, boolean cancellation) {
        List<String> accounts = new ArrayList<>(collectedAccounts());
        accounts.add(pendingAccount());
        String in = String.join(",", Collections.nCopies(accounts.size(), "?"));
        List<Object> args = new ArrayList<>(accounts);
        args.add(documentNumber);
        // La contre-passation d'un document annule porte la source du document ; reversal_of la distingue.
        String types = "('INVOICE_ISSUED', 'CREDIT_NOTE_ISSUED', 'INVOICE_CANCELLED') AND e.reversal_of IS " + (cancellation ? "NOT NULL" : "NULL");
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM acc_entry e WHERE e.source_code = ? AND e.source_type IN " + types
                + " AND e.status IN ('VALIDATED', 'REVERSED')", Long.class, documentNumber);
        if (count == null || count == 0) {
            return null;
        }
        BigDecimal value = jdbc.queryForObject("SELECT COALESCE(SUM(l.credit - l.debit), 0) FROM acc_entry_line l JOIN acc_entry e ON e.id = l.entry_id "
                + "WHERE l.account_number IN (" + in + ") AND e.source_code = ? AND e.source_type IN " + types
                + " AND e.status IN ('VALIDATED', 'REVERSED')", BigDecimal.class, args.toArray());
        return money(value);
    }

    private BigDecimal rate(String currency, BigDecimal documentRate, LocalDate date, String number) {
        String code = StringUtils.hasText(currency) ? currency.trim().toUpperCase(Locale.ROOT) : ExchangeRateService.BASE;
        if (ExchangeRateService.BASE.equals(code)) {
            return null;
        }
        if (documentRate != null && documentRate.signum() > 0) {
            return documentRate;
        }
        return rates.rate(code, date == null ? LocalDate.now() : date)
                .orElseThrow(() -> new BadRequestException("Aucun taux " + code + " pour le document " + number));
    }

    private static BigDecimal xaf(BigDecimal value, BigDecimal rate) {
        return value.multiply(rate).setScale(0, RoundingMode.HALF_UP).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal revenue(LocalDate from, LocalDate to) {
        return money(jdbc.queryForObject("SELECT COALESCE(SUM(l.credit - l.debit), 0) FROM acc_entry_line l JOIN acc_entry e ON e.id = l.entry_id "
                + "WHERE l.account_number LIKE '70%' AND e.status IN ('VALIDATED', 'REVERSED') AND e.entry_date BETWEEN ? AND ?",
                BigDecimal.class, Date.valueOf(from), Date.valueOf(to)));
    }

    /** Solde debiteur d'un compte avant une date. */
    private BigDecimal balanceBefore(String account, LocalDate date) {
        return money(jdbc.queryForObject("SELECT COALESCE(SUM(l.debit - l.credit), 0) FROM acc_entry_line l JOIN acc_entry e ON e.id = l.entry_id "
                + "WHERE l.account_number = ? AND e.status IN ('VALIDATED', 'REVERSED') AND e.entry_date < ?", BigDecimal.class, account, Date.valueOf(date)));
    }

    /** Solde debiteur d'un compte a une date incluse. */
    private BigDecimal balanceUpTo(String account, LocalDate date) {
        return balanceBefore(account, date.plusDays(1));
    }

    private String accountLabel(String account) {
        List<String> labels = jdbc.queryForList("SELECT label FROM acc_account WHERE number = ?", String.class, account);
        return labels.isEmpty() ? account : labels.get(0);
    }

    // ------------------------------------------------------------------ editions

    /** PDF de la declaration : numero DTV a la premiere impression d'une declaration validee ou liquidee. */
    public byte[] pdf(Long id) {
        require("VAT_RETURN:READ");
        AccVatReturn record = get(id);
        Detail detail = read(record.getDetailJson());
        VoucherService.Issued issued;
        if ("DRAFT".equals(record.getStatus())) {
            issued = new VoucherService.Issued("BROUILLON-" + record.getPeriodCode(), "DTV", record.getPeriodCode(), record.getDueAmount(), null,
                    record.getPreparedBy(), record.getPreparedAt(), 0, null, null);
        } else {
            issued = vouchers.issueAndPrint("DTV", SOURCE, record.getPeriodCode(), record.getDueAmount(), "Declaration de TVA " + record.getPeriodCode(),
                    actors.resolve("SYSTEM"));
            if (record.getVoucherNumber() == null) {
                record.setVoucherNumber(issued.number());
                repository.save(record);
            }
        }
        List<VoucherService.Section> sections = new ArrayList<>();
        sections.add(new VoucherService.Section("Declaration").pair("Periode", record.getPeriodCode() + " (du " + record.getStartDate() + " au " + record.getEndDate() + ")")
                .pair("Regime d'exigibilite", "PAYMENT".equals(record.getRegime()) ? "Sur les encaissements" : "Sur les debits (a la facture)")
                .pair("Etat", label(record.getStatus())).pair("Piece de liquidation", record.getLiquidationEntryNumber())
                .pair("Chiffre d'affaires HT", VoucherService.money(record.getRevenue())));
        sections.add(new VoucherService.Section("Calcul").columns(List.of("Rubrique", "Montant"), List.of(false, true))
                .row("TVA collectee", VoucherService.money(record.getCollected()))
                .row("- TVA deductible", VoucherService.money(record.getDeductible()))
                .row("- Credit de TVA reporte du mois precedent", VoucherService.money(record.getCreditBroughtForward()))
                .row("= TVA due", VoucherService.money(record.getDueAmount()))
                .row("Credit de TVA a reporter", VoucherService.money(record.getCreditCarriedForward()))
                .row("TVA encore en attente d'exigibilite", VoucherService.money(record.getPendingBalance())));
        VoucherService.Section accounts = new VoucherService.Section("Par compte").columns(List.of("Compte", "Libelle", "Nature", "Montant"), List.of(false, false, false, true));
        detail.collected().forEach(line -> accounts.row(line.account(), line.label(), "Collectee", VoucherService.money(line.amount())));
        detail.deductible().forEach(line -> accounts.row(line.account(), line.label(), "Deductible", VoucherService.money(line.amount())));
        sections.add(accounts);
        VoucherService.Section codes = new VoucherService.Section("TVA collectee par code de taxe").columns(List.of("Code", "Base", "Taxe"), List.of(false, true, true));
        detail.byCode().forEach(line -> codes.row(line.taxCode(), VoucherService.money(line.base()), VoucherService.money(line.tax())));
        sections.add(codes);
        VoucherService.Section documents = new VoucherService.Section("Detail par facture").columns(
                List.of("Date", "Document", "Fait", "Client", "Devise", "Taxe (XAF)", "Comptabilise", "Ecart"), List.of(false, false, false, false, false, true, true, true));
        detail.documents().forEach(line -> documents.row(line.date(), line.documentNumber(), "ANNULATION".equals(line.event()) ? "Annulation" : type(line.documentType()),
                line.customerName(), line.currency(), VoucherService.money(line.taxXaf()), line.accountingTax() == null ? "Non" : VoucherService.money(line.accountingTax()),
                line.difference() == null ? "" : VoucherService.money(line.difference())));
        documents.row("", "", "", "Dont documents scelles", "", VoucherService.money(detail.sealedTaxTotal()), "", "");
        if (record.getControlDifference() != null) {
            documents.row("", "", "", "Total", "", VoucherService.money(record.getInvoiceTaxTotal()), VoucherService.money(record.getCollected()),
                    VoucherService.money(record.getControlDifference()));
        }
        sections.add(documents);
        return vouchers.render("Declaration de TVA " + record.getPeriodCode(), issued, sections, record.getDueAmount(), null, List.of(
                new VoucherService.Signature("Prepare par", record.getPreparedBy(), VoucherService.date(record.getPreparedAt())),
                new VoucherService.Signature("Valide par", record.getValidatedBy(), record.getValidatedAt() == null ? null : VoucherService.date(record.getValidatedAt()))));
    }

    /** Export CSV (separateur point-virgule, UTF-8 avec BOM) : synthese puis detail par facture. */
    @Transactional(readOnly = true)
    public byte[] csv(Long id) {
        require("VAT_RETURN:READ");
        AccVatReturn record = get(id);
        Detail detail = read(record.getDetailJson());
        StringBuilder out = new StringBuilder("﻿");
        out.append("Periode;Regime;TVA collectee;TVA deductible;Credit reporte;TVA due;Credit a reporter;Chiffre d'affaires HT;Etat\n");
        out.append(String.join(";", record.getPeriodCode(), record.getRegime(), plain(record.getCollected()), plain(record.getDeductible()),
                plain(record.getCreditBroughtForward()), plain(record.getDueAmount()), plain(record.getCreditCarriedForward()), plain(record.getRevenue()),
                record.getStatus())).append("\n\n");
        out.append("Date;Document;Numero fiscal;Scelle;Type;Fait;Client;Devise;Base HT;TVA;Centime additionnel;Taxe XAF;Comptabilise;Ecart\n");
        for (DocumentTax line : detail.documents()) {
            out.append(String.join(";", String.valueOf(line.date()), line.documentNumber(), line.fiscalNumber() == null ? "" : line.fiscalNumber(),
                    line.sealed() ? "oui" : "non", line.documentType(), line.event(), csvText(line.customerName()),
                    String.valueOf(line.currency()), plain(line.taxable()), plain(line.vat()), plain(line.extraTax()), plain(line.taxXaf()),
                    line.accountingTax() == null ? "" : plain(line.accountingTax()), line.difference() == null ? "" : plain(line.difference()))).append("\n");
        }
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------ outils

    private void require(String authority) {
        if (!actors.hasAuthority(authority)) {
            throw new ForbiddenException("Action reservee : " + authority);
        }
    }

    private static YearMonth month(String period) {
        try {
            return YearMonth.parse(period.trim());
        } catch (RuntimeException exception) {
            throw new BadRequestException("Periode attendue au format AAAA-MM");
        }
    }

    private static Detail read(String json) {
        if (json == null) {
            return new Detail(List.of(), List.of(), List.of(), List.of(), BigDecimal.ZERO);
        }
        try {
            return JSON.readValue(json, Detail.class);
        } catch (Exception exception) {
            throw new IllegalStateException("Detail de la declaration illisible : " + exception.getMessage(), exception);
        }
    }

    private static String label(String status) {
        return switch (status) {
            case "VALIDATED" -> "validee";
            case "LIQUIDATED" -> "liquidee";
            default -> "en brouillon";
        };
    }

    private static String type(String documentType) {
        return "CREDIT_NOTE".equals(documentType) ? "Avoir" : "Facture";
    }

    private static BigDecimal sum(List<AccountAmount> lines) {
        return lines.stream().map(AccountAmount::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(4, RoundingMode.HALF_UP);
    }

    private static String plain(BigDecimal value) {
        return value == null ? "" : value.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private static String csvText(String value) {
        return value == null ? "" : value.replace(";", ",").replace("\n", " ");
    }
}
