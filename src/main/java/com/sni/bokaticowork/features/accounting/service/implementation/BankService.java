package com.sni.bokaticowork.features.accounting.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.enums.EntryOrigin;
import com.sni.bokaticowork.features.accounting.enums.EntryStatus;
import com.sni.bokaticowork.features.accounting.model.*;
import com.sni.bokaticowork.features.accounting.repository.*;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.fund.voucher.VoucherService;
import com.sni.bokaticowork.features.payment.dto.request.PayInvoiceRequest;
import com.sni.bokaticowork.features.payment.enums.PaymentMethod;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentService;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Banque et rapprochement (lot F10, rapprochement-et-comptes-tiers section 1) : comptes bancaires
 * et mobile money, modeles d'import, releves controles et figes, pointage automatique (reference,
 * montant et date, combinaisons) et manuel, depointage trace, comptabilisation des lignes sans
 * ecriture (regles d'affectation, virement client transforme en paiement), etat de rapprochement
 * prepare, valide par une autre personne avec un ecart nul, puis fige.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class BankService {

    private final BankAccountRepository accountRepository;
    private final BankImportTemplateRepository templateRepository;
    private final BankStatementRepository statementRepository;
    private final BankStatementLineRepository statementLineRepository;
    private final BankMatchingRepository matchingRepository;
    private final BankAssignmentRuleRepository ruleRepository;
    private final BankReconciliationRepository reconciliationRepository;
    private final AccEntryLineRepository entryLineRepository;
    private final AccountingEntryService entries;
    private final AccountingChartService chart;
    private final AccountingSettings settings;
    private final CashActorResolver actors;
    private final VoucherService vouchers;
    @Lazy
    private final PaymentService paymentService;
    private final DataSource dataSource;

    // ------------------------------------------------------------------ vues

    public record Line(Long id, String statementNumber, LocalDate date, LocalDate valueDate, String label, String reference, BigDecimal amount,
                       String matchingCode, String awaitingPayment, String postedEntry) {
    }

    public record BookLine(Long id, String entryNumber, LocalDate date, String label, String reference, String sourceCode, BigDecimal amount,
                           String matchingCode) {
    }

    public record Proposal(List<Long> statementLineIds, List<Long> entryLineIds, BigDecimal amount, String kind, int confidence) {
    }

    public record MatchingScreen(String bankAccountCode, List<Line> statementLines, List<BookLine> bookLines, List<Proposal> proposals,
                                 BigDecimal statementOpen, BigDecimal bookOpen) {
    }

    public record ImportResult(BankStatement statement, int lines, int duplicates, int autoMatched, List<String> warnings) {
    }

    public record ManualLine(LocalDate date, LocalDate valueDate, String label, String reference, BigDecimal amount) {
    }

    public record ManualStatement(LocalDate periodFrom, LocalDate periodTo, BigDecimal openingBalance, BigDecimal closingBalance, List<ManualLine> lines) {
    }

    public record PostLine(Long ruleId, String accountNumber, String thirdParty, String label, String invoiceNumber) {
    }

    public record Reconciliation(String number, String bankAccountCode, LocalDate date, BigDecimal statementBalance, BigDecimal bookBalance,
                                 List<BookLine> bookOutstanding, List<Line> statementOutstanding, BigDecimal reconciledBalance, BigDecimal difference,
                                 String status, String preparedBy, String validatedBy, Instant validatedAt, Long id) {
    }

    // ------------------------------------------------------------------ comptes et modeles

    @Transactional(readOnly = true)
    public List<BankAccount> accounts() {
        return accountRepository.findAllByOrderByCodeAsc();
    }

    /** Compte bancaire : le compte 521x et son journal sont crees s'ils n'existent pas. */
    public BankAccount saveAccount(BankAccount input) {
        require("BANK_ACCOUNT:MANAGE");
        BankAccount account = accountRepository.findByCode(AnalyticService.code(input.getCode()))
                .orElseGet(() -> BankAccount.builder().code(AnalyticService.code(input.getCode())).createdBy(actors.resolve("SYSTEM")).build());
        if (!StringUtils.hasText(input.getLabel())) {
            throw new BadRequestException("Donnez un libelle au compte");
        }
        account.setLabel(input.getLabel().trim());
        account.setKind(input.getKind() == null ? "BANK" : input.getKind().toUpperCase(Locale.ROOT));
        account.setBankName(input.getBankName());
        account.setBranch(input.getBranch());
        account.setRib(input.getRib());
        account.setCurrency(input.getCurrency() == null ? "XAF" : input.getCurrency().toUpperCase(Locale.ROOT));
        account.setImportTemplateCode(input.getImportTemplateCode());
        account.setOpeningBalance(input.getOpeningBalance() == null ? BigDecimal.ZERO : input.getOpeningBalance());
        account.setOpeningDate(input.getOpeningDate());
        account.setActive(input.getActive() == null || input.getActive());
        if (account.getAccountNumber() == null) {
            String number = StringUtils.hasText(input.getAccountNumber()) ? input.getAccountNumber().trim() : null;
            if (number == null || chart.find(number).isEmpty()) {
                number = chart.create(new AccountingRequests.CreateAccount("521", number, account.getLabel(), null, null,
                        "MOBILE_MONEY".equals(account.getKind()) ? com.sni.bokaticowork.features.accounting.enums.TreasuryType.MOBILE_MONEY
                                : com.sni.bokaticowork.features.accounting.enums.TreasuryType.BANK, null, null, null,
                        "Compte bancaire " + account.getCode())).number();
            }
            account.setAccountNumber(number);
            String journal = ("MOBILE_MONEY".equals(account.getKind()) ? "MM-" : "BQ-") + number;
            account.setJournalCode(journal);
        }
        return accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public List<BankImportTemplate> templates() {
        return templateRepository.findAll();
    }

    public BankImportTemplate saveTemplate(BankImportTemplate input) {
        require("BANK_ACCOUNT:MANAGE");
        BankImportTemplate template = templateRepository.findByCode(AnalyticService.code(input.getCode()))
                .orElseGet(() -> BankImportTemplate.builder().code(AnalyticService.code(input.getCode())).build());
        if (!StringUtils.hasText(input.getDateColumn()) || !StringUtils.hasText(input.getLabelColumn())
                || (!StringUtils.hasText(input.getAmountColumn()) && (!StringUtils.hasText(input.getDebitColumn()) || !StringUtils.hasText(input.getCreditColumn())))) {
            throw new BadRequestException("Un modele indique les colonnes de date, de libelle et de montant (signe, ou debit et credit)");
        }
        template.setLabel(StringUtils.hasText(input.getLabel()) ? input.getLabel() : template.getCode());
        template.setDelimiter(StringUtils.hasText(input.getDelimiter()) ? input.getDelimiter() : ";");
        template.setHeaderRows(input.getHeaderRows() == null ? 1 : input.getHeaderRows());
        template.setDateColumn(input.getDateColumn());
        template.setValueDateColumn(input.getValueDateColumn());
        template.setLabelColumn(input.getLabelColumn());
        template.setReferenceColumn(input.getReferenceColumn());
        template.setAmountColumn(input.getAmountColumn());
        template.setDebitColumn(input.getDebitColumn());
        template.setCreditColumn(input.getCreditColumn());
        template.setDateFormat(StringUtils.hasText(input.getDateFormat()) ? input.getDateFormat() : "dd/MM/yyyy");
        template.setDecimalSeparator(StringUtils.hasText(input.getDecimalSeparator()) ? input.getDecimalSeparator() : ",");
        template.setEncoding(StringUtils.hasText(input.getEncoding()) ? input.getEncoding() : "UTF-8");
        return templateRepository.save(template);
    }

    /** Lecture d'un fichier par un modele, sans rien enregistrer (essai du modele). */
    @Transactional(readOnly = true)
    public List<ManualLine> preview(String templateCode, byte[] content) {
        return parse(templateRepository.findByCode(AnalyticService.code(templateCode))
                .orElseThrow(() -> new ResourceNotFoundException("Modele d'import introuvable : " + templateCode)), content);
    }

    List<ManualLine> parse(BankImportTemplate template, byte[] content) {
        String text = new String(content, Charset.forName(template.getEncoding()));
        if (text.startsWith("﻿")) {
            text = text.substring(1);
        }
        List<String> rows = Arrays.stream(text.split("\r?\n")).filter(StringUtils::hasText).toList();
        if (rows.size() <= template.getHeaderRows()) {
            throw new BadRequestException("Le fichier ne contient aucune ligne de releve");
        }
        String delimiter = Pattern.quote(template.getDelimiter());
        List<String> header = template.getHeaderRows() > 0 ? split(rows.get(template.getHeaderRows() - 1), delimiter) : List.of();
        DateTimeFormatter format = DateTimeFormatter.ofPattern(template.getDateFormat());
        List<ManualLine> lines = new ArrayList<>();
        for (int index = template.getHeaderRows(); index < rows.size(); index++) {
            List<String> cells = split(rows.get(index), delimiter);
            try {
                LocalDate date = LocalDate.parse(cell(cells, header, template.getDateColumn()), format);
                String valueText = template.getValueDateColumn() == null ? null : cell(cells, header, template.getValueDateColumn());
                LocalDate valueDate = StringUtils.hasText(valueText) ? LocalDate.parse(valueText, format) : null;
                BigDecimal amount;
                if (StringUtils.hasText(template.getAmountColumn())) {
                    amount = number(cell(cells, header, template.getAmountColumn()), template.getDecimalSeparator());
                } else {
                    amount = number(cell(cells, header, template.getCreditColumn()), template.getDecimalSeparator())
                            .subtract(number(cell(cells, header, template.getDebitColumn()), template.getDecimalSeparator()));
                }
                if (amount.signum() == 0) {
                    continue;
                }
                lines.add(new ManualLine(date, valueDate, cell(cells, header, template.getLabelColumn()),
                        template.getReferenceColumn() == null ? null : cell(cells, header, template.getReferenceColumn()), amount));
            } catch (RuntimeException exception) {
                throw new BadRequestException("Ligne " + (index + 1) + " du fichier illisible : " + exception.getMessage());
            }
        }
        return lines;
    }

    private static List<String> split(String row, String delimiter) {
        return Arrays.stream(row.split(delimiter, -1)).map(value -> value.trim().replaceAll("^\"|\"$", "").trim()).toList();
    }

    private static String cell(List<String> cells, List<String> header, String column) {
        int index;
        if (column.matches("\\d+")) {
            index = Integer.parseInt(column);
        } else {
            index = -1;
            for (int i = 0; i < header.size(); i++) {
                if (header.get(i).equalsIgnoreCase(column.trim())) {
                    index = i;
                }
            }
            if (index < 0) {
                throw new BadRequestException("Colonne " + column + " absente de l'en-tete");
            }
        }
        return index < cells.size() ? cells.get(index) : "";
    }

    private static BigDecimal number(String value, String decimal) {
        if (!StringUtils.hasText(value)) {
            return BigDecimal.ZERO;
        }
        String cleaned = value.replace(" ", "").replace(" ", "").replace(" ", "");
        cleaned = ",".equals(decimal) ? cleaned.replace(".", "").replace(",", ".") : cleaned.replace(",", "");
        return new BigDecimal(cleaned);
    }

    // ------------------------------------------------------------------ releves

    public ImportResult importFile(String accountCode, byte[] content, String fileName, BigDecimal openingBalance, BigDecimal closingBalance,
                                   String replaceNumber) {
        BankAccount account = account(accountCode);
        if (account.getImportTemplateCode() == null) {
            throw new BadRequestException("Le compte " + account.getCode() + " n'a pas de modele d'import");
        }
        List<ManualLine> lines = parse(templateRepository.findByCode(account.getImportTemplateCode()).orElseThrow(), content);
        LocalDate from = lines.stream().map(ManualLine::date).min(LocalDate::compareTo).orElseThrow();
        LocalDate to = lines.stream().map(ManualLine::date).max(LocalDate::compareTo).orElseThrow();
        return store(account, new ManualStatement(from, to, openingBalance, closingBalance, lines), fileName, "FILE", replaceNumber);
    }

    public ImportResult importManual(String accountCode, ManualStatement statement) {
        return store(account(accountCode), statement, null, "MANUAL", null);
    }

    private ImportResult store(BankAccount account, ManualStatement input, String fileName, String source, String replaceNumber) {
        require("BANK_STATEMENT:IMPORT");
        if (input.lines() == null || input.lines().isEmpty()) {
            throw new BadRequestException("Un releve a au moins une ligne");
        }
        List<String> warnings = new ArrayList<>();
        Optional<BankStatement> previous = statementRepository.findByBankAccountCodeAndStatusOrderByPeriodToDesc(account.getCode(), "IMPORTED").stream()
                .filter(statement -> replaceNumber == null || !statement.getNumber().equals(replaceNumber)).findFirst();
        BigDecimal opening = input.openingBalance() != null ? input.openingBalance()
                : previous.map(BankStatement::getClosingBalance).orElse(account.getOpeningBalance());
        boolean missing = false;
        if (previous.isPresent()) {
            if (previous.get().getClosingBalance().compareTo(opening) != 0) {
                missing = true;
                warnings.add("Le solde initial (" + plain(opening) + ") differe du solde final du releve precedent ("
                        + plain(previous.get().getClosingBalance()) + ") : releve manquant ?");
            }
            if (input.periodFrom().isAfter(previous.get().getPeriodTo().plusDays(1))) {
                missing = true;
                warnings.add("Periode manquante entre le " + previous.get().getPeriodTo() + " et le " + input.periodFrom());
            }
            if (missing && "REFUSE".equalsIgnoreCase(settings.text("accounting.bank.missing-statement", "ALERT"))) {
                throw new BadRequestException("Releve refuse : " + String.join(" ; ", warnings));
            }
        }
        BigDecimal sum = input.lines().stream().map(ManualLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal closing = input.closingBalance() != null ? input.closingBalance() : opening.add(sum);
        if (opening.add(sum).compareTo(closing) != 0) {
            throw new BadRequestException("La somme des lignes (" + plain(sum) + ") ne fait pas passer du solde initial (" + plain(opening)
                    + ") au solde final (" + plain(closing) + ")");
        }
        String number = "RLV-" + account.getCode() + "-" + (statementRepository.count() + 1);
        while (statementRepository.findByNumber(number).isPresent()) {
            number = number + "B";
        }
        BankStatement statement = statementRepository.save(BankStatement.builder().number(number).bankAccountCode(account.getCode())
                .periodFrom(input.periodFrom()).periodTo(input.periodTo()).openingBalance(opening).closingBalance(closing).fileName(fileName)
                .source(source).missingBefore(missing).importedBy(actors.resolve("SYSTEM")).build());
        if (replaceNumber != null) {
            statementRepository.findByNumber(replaceNumber).ifPresent(old -> {
                old.setStatus("REPLACED");
                old.setReplacedBy(statement.getNumber());
                statementRepository.save(old);
            });
        }
        int duplicates = 0;
        int lineNo = 1;
        for (ManualLine line : input.lines()) {
            String fingerprint = fingerprint(line);
            if (statementLineRepository.existsByBankAccountCodeAndFingerprint(account.getCode(), fingerprint)) {
                duplicates++;
                continue;
            }
            statementLineRepository.save(BankStatementLine.builder().statementId(statement.getId()).bankAccountCode(account.getCode()).lineNo(lineNo++)
                    .operationDate(line.date()).valueDate(line.valueDate()).label(line.label() == null ? "" : line.label())
                    .reference(StringUtils.hasText(line.reference()) ? line.reference().trim() : null).amount(line.amount().setScale(4, RoundingMode.HALF_UP))
                    .fingerprint(fingerprint).build());
        }
        if (duplicates > 0) {
            warnings.add(duplicates + " ligne(s) deja importee(s) ignoree(s)");
        }
        statement.setDuplicates(duplicates);
        statementRepository.save(statement);
        statementLineRepository.flush();
        int matched = autoMatch(account.getCode());
        return new ImportResult(statement, lineNo - 1, duplicates, matched, warnings);
    }

    private static String fingerprint(ManualLine line) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String raw = line.date() + "|" + line.amount().setScale(2, RoundingMode.HALF_UP).toPlainString() + "|"
                    + (line.label() == null ? "" : line.label().trim().toUpperCase(Locale.ROOT)) + "|" + (line.reference() == null ? "" : line.reference().trim());
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Transactional(readOnly = true)
    public List<BankStatement> statements(String accountCode) {
        return statementRepository.findByBankAccountCodeAndStatusOrderByPeriodToDesc(AnalyticService.code(accountCode), "IMPORTED");
    }

    // ------------------------------------------------------------------ pointage

    List<BookLine> openBookLines(BankAccount account, LocalDate until) {
        List<BookLine> lines = new ArrayList<>();
        new JdbcTemplate(dataSource).query("SELECT l.id, COALESCE(e.entry_number, e.provisional_number), e.entry_date, l.label, l.reference, e.source_code, "
                        + "l.debit - l.credit, l.matching_code FROM acc_entry_line l JOIN acc_entry e ON e.id = l.entry_id "
                        + "WHERE e.status IN ('VALIDATED', 'REVERSED') AND l.account_number = ? AND l.matching_code IS NULL "
                        + (until == null ? "" : "AND e.entry_date <= ? ") + "ORDER BY e.entry_date, l.id",
                rs -> {
                    lines.add(new BookLine(rs.getLong(1), rs.getString(2), rs.getDate(3).toLocalDate(), rs.getString(4), rs.getString(5),
                            rs.getString(6), rs.getBigDecimal(7), rs.getString(8)));
                }, until == null ? new Object[]{account.getAccountNumber()} : new Object[]{account.getAccountNumber(), java.sql.Date.valueOf(until)});
        // Ce qui precede le debut du suivi est couvert par le solde d'ouverture du compte : ni suspens, ni a pointer.
        if (account.getOpeningDate() != null) {
            lines.removeIf(line -> line.date().isBefore(account.getOpeningDate()));
        }
        return lines;
    }

    @Transactional(readOnly = true)
    public MatchingScreen screen(String accountCode) {
        BankAccount account = account(accountCode);
        List<BankStatementLine> open = statementLineRepository.findByBankAccountCodeAndMatchingCodeIsNullOrderByOperationDateAsc(account.getCode());
        List<BookLine> book = openBookLines(account, null);
        List<Proposal> proposals = proposals(open, book);
        return new MatchingScreen(account.getCode(), open.stream().map(this::view).toList(), book, proposals,
                open.stream().map(BankStatementLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
                book.stream().map(BookLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /**
     * Pointage automatique : reference exacte, puis montant exact a date proche avec correspondance
     * unique, puis combinaisons (une ligne de releve egale a plusieurs ecritures). Les
     * correspondances sures sont pointees d'office (parametre), les autres restent proposees.
     */
    public int autoMatch(String accountCode) {
        BankAccount account = account(accountCode);
        boolean auto = !"false".equalsIgnoreCase(settings.text("accounting.bank.auto-match", "true"));
        if (!auto) {
            return 0;
        }
        int matched = 0;
        List<BankStatementLine> open = new ArrayList<>(statementLineRepository.findByBankAccountCodeAndMatchingCodeIsNullOrderByOperationDateAsc(account.getCode()));
        List<BookLine> book = new ArrayList<>(openBookLines(account, null));
        for (Proposal proposal : proposals(open, book)) {
            if (proposal.confidence() < 90) {
                continue;
            }
            Set<Long> s = new HashSet<>(proposal.statementLineIds());
            Set<Long> b = new HashSet<>(proposal.entryLineIds());
            boolean stillOpen = open.stream().filter(line -> s.contains(line.getId())).count() == s.size()
                    && book.stream().filter(line -> b.contains(line.id())).count() == b.size();
            if (!stillOpen) {
                continue;
            }
            match(account, proposal.statementLineIds(), proposal.entryLineIds(), proposal.kind(), "SYSTEM");
            open.removeIf(line -> s.contains(line.getId()));
            book.removeIf(line -> b.contains(line.id()));
            matched += s.size();
        }
        return matched;
    }

    List<Proposal> proposals(List<BankStatementLine> open, List<BookLine> book) {
        int window = (int) Math.max(0, parseLong(settings.text("accounting.bank.date-window-days", "5"), 5));
        int maxLines = (int) Math.max(2, parseLong(settings.text("accounting.bank.max-combined-lines", "10"), 10));
        List<Proposal> proposals = new ArrayList<>();
        Set<Long> usedS = new HashSet<>();
        Set<Long> usedB = new HashSet<>();
        // 1. Reference exacte.
        for (BankStatementLine line : open) {
            String haystack = ((line.getLabel() == null ? "" : line.getLabel()) + " " + (line.getReference() == null ? "" : line.getReference())
                    + " " + (line.getAwaitingPayment() == null ? "" : line.getAwaitingPayment())).toUpperCase(Locale.ROOT);
            List<BookLine> candidates = book.stream().filter(b -> !usedB.contains(b.id()) && b.amount().compareTo(line.getAmount()) == 0)
                    .filter(b -> contains(haystack, b.reference()) || contains(haystack, b.sourceCode()) || contains(haystack, b.entryNumber()))
                    .toList();
            if (candidates.size() == 1) {
                proposals.add(new Proposal(List.of(line.getId()), List.of(candidates.get(0).id()), line.getAmount(), "AUTO_REFERENCE", 100));
                usedS.add(line.getId());
                usedB.add(candidates.get(0).id());
            }
        }
        // 2. Montant exact, date proche, correspondance unique dans les deux sens.
        for (BankStatementLine line : open) {
            if (usedS.contains(line.getId())) {
                continue;
            }
            List<BookLine> candidates = book.stream().filter(b -> !usedB.contains(b.id()) && b.amount().compareTo(line.getAmount()) == 0
                    && Math.abs(ChronoUnit.DAYS.between(b.date(), line.getOperationDate())) <= window).toList();
            if (candidates.isEmpty()) {
                continue;
            }
            BookLine candidate = candidates.get(0);
            long rivals = open.stream().filter(other -> !usedS.contains(other.getId()) && other.getAmount().compareTo(line.getAmount()) == 0
                    && Math.abs(ChronoUnit.DAYS.between(candidate.date(), other.getOperationDate())) <= window).count();
            int confidence = candidates.size() == 1 && rivals == 1 ? 90 : 60;
            proposals.add(new Proposal(List.of(line.getId()), List.of(candidate.id()), line.getAmount(), "AUTO_AMOUNT", confidence));
            if (confidence >= 90) {
                usedS.add(line.getId());
                usedB.add(candidate.id());
            }
        }
        // 3. Combinaison : une ligne de releve egale a la somme de plusieurs ecritures (meme sens, fenetre de dates).
        for (BankStatementLine line : open) {
            if (usedS.contains(line.getId())) {
                continue;
            }
            List<BookLine> pool = book.stream().filter(b -> !usedB.contains(b.id()) && b.amount().signum() == line.getAmount().signum()
                    && b.amount().abs().compareTo(line.getAmount().abs()) < 0
                    && Math.abs(ChronoUnit.DAYS.between(b.date(), line.getOperationDate())) <= window).limit(20).toList();
            List<BookLine> combination = subset(pool, line.getAmount(), maxLines);
            if (combination != null) {
                proposals.add(new Proposal(List.of(line.getId()), combination.stream().map(BookLine::id).toList(), line.getAmount(), "AUTO_COMBINATION", 90));
                usedS.add(line.getId());
                combination.forEach(b -> usedB.add(b.id()));
            }
        }
        return proposals;
    }

    /** Sous-ensemble (2 a max lignes) de somme egale a la cible, recherche bornee. */
    private static List<BookLine> subset(List<BookLine> pool, BigDecimal target, int max) {
        Deque<BookLine> stack = new ArrayDeque<>();
        return search(pool, 0, target, max, stack, new int[]{0});
    }

    private static List<BookLine> search(List<BookLine> pool, int start, BigDecimal remaining, int max, Deque<BookLine> stack, int[] steps) {
        if (remaining.signum() == 0 && stack.size() >= 2) {
            return new ArrayList<>(stack);
        }
        if (stack.size() >= max || ++steps[0] > 20000) {
            return null;
        }
        for (int index = start; index < pool.size(); index++) {
            BookLine candidate = pool.get(index);
            BigDecimal next = remaining.subtract(candidate.amount());
            if (next.signum() != 0 && next.signum() != remaining.signum()) {
                continue;
            }
            stack.push(candidate);
            List<BookLine> found = search(pool, index + 1, next, max, stack, steps);
            if (found != null) {
                return found;
            }
            stack.pop();
        }
        return null;
    }

    private static boolean contains(String haystack, String needle) {
        return StringUtils.hasText(needle) && needle.trim().length() >= 4 && haystack.contains(needle.trim().toUpperCase(Locale.ROOT));
    }

    /** Pointage manuel : les deux cotes ont le meme total. */
    public BankMatching matchManual(String accountCode, List<Long> statementLineIds, List<Long> entryLineIds) {
        require("BANK_RECONCILIATION:MANAGE");
        return match(account(accountCode), statementLineIds, entryLineIds, "MANUAL", actors.resolve("SYSTEM"));
    }

    BankMatching match(BankAccount account, List<Long> statementLineIds, List<Long> entryLineIds, String kind, String actor) {
        if (statementLineIds == null || statementLineIds.isEmpty() || entryLineIds == null || entryLineIds.isEmpty()) {
            throw new BadRequestException("Un pointage associe au moins une ligne de releve et une ligne d'ecriture");
        }
        List<BankStatementLine> lines = statementLineRepository.findAllById(statementLineIds);
        List<AccEntryLine> book = entryLineRepository.findAllById(entryLineIds);
        if (lines.size() != statementLineIds.size() || book.size() != entryLineIds.size()) {
            throw new ResourceNotFoundException("Ligne de releve ou d'ecriture introuvable");
        }
        for (BankStatementLine line : lines) {
            if (!line.getBankAccountCode().equals(account.getCode()) || line.getMatchingCode() != null) {
                throw new BadRequestException("La ligne de releve " + line.getId() + " n'est pas a pointer sur ce compte");
            }
        }
        for (AccEntryLine line : book) {
            if (!line.getAccountNumber().equals(account.getAccountNumber()) || line.getMatchingCode() != null) {
                throw new BadRequestException("La ligne d'ecriture " + line.getId() + " n'est pas a pointer sur le compte " + account.getAccountNumber());
            }
        }
        BigDecimal statementTotal = lines.stream().map(BankStatementLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal bookTotal = book.stream().map(line -> line.getDebit().subtract(line.getCredit())).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (statementTotal.compareTo(bookTotal) != 0) {
            throw new BadRequestException("Les totaux ne sont pas egaux : releve " + plain(statementTotal) + ", ecritures " + plain(bookTotal));
        }
        String code = "P" + String.format("%06d", matchingRepository.countByBankAccountCode(account.getCode()) + 1);
        BankMatching matching = matchingRepository.save(BankMatching.builder().code(code).bankAccountCode(account.getCode()).kind(kind)
                .statementLineIds(join(statementLineIds)).entryLineIds(join(entryLineIds)).amount(statementTotal).createdBy(actor).build());
        Instant now = Instant.now();
        lines.forEach(line -> {
            line.setMatchingCode(code);
            line.setMatchedAt(now);
            line.setMatchedBy(actor);
            statementLineRepository.save(line);
        });
        book.forEach(line -> {
            line.setMatchingCode(code);
            entryLineRepository.save(line);
        });
        return matching;
    }

    /** Depointage motive ; un pointage d'un rapprochement valide demande BANK_RECONCILIATION:UNLOCK. */
    public BankMatching unmatch(String accountCode, String code, String reason) {
        require("BANK_RECONCILIATION:MANAGE");
        if (!StringUtils.hasText(reason)) {
            throw new BadRequestException("Indiquez le motif du depointage");
        }
        BankAccount account = account(accountCode);
        BankMatching matching = matchingRepository.findByBankAccountCodeAndCode(account.getCode(), code)
                .orElseThrow(() -> new ResourceNotFoundException("Pointage introuvable : " + code));
        if (matching.getUnmatchedAt() != null) {
            throw new BadRequestException("Ce pointage est deja defait");
        }
        if (Boolean.TRUE.equals(matching.getLocked()) && !actors.hasAuthority("BANK_RECONCILIATION:UNLOCK")) {
            throw new ForbiddenException("Ce pointage appartient a un rapprochement valide : depointer demande BANK_RECONCILIATION:UNLOCK");
        }
        statementLineRepository.findAllById(ids(matching.getStatementLineIds())).forEach(line -> {
            line.setMatchingCode(null);
            line.setMatchedAt(null);
            line.setMatchedBy(null);
            statementLineRepository.save(line);
        });
        entryLineRepository.findAllById(ids(matching.getEntryLineIds())).forEach(line -> {
            line.setMatchingCode(null);
            entryLineRepository.save(line);
        });
        matching.setUnmatchedAt(Instant.now());
        matching.setUnmatchedBy(actors.resolve("SYSTEM"));
        matching.setUnmatchedReason(reason.trim());
        return matchingRepository.save(matching);
    }

    // ------------------------------------------------------------------ lignes sans ecriture

    @Transactional(readOnly = true)
    public List<BankAssignmentRule> rules() {
        return ruleRepository.findAllByOrderByPriorityAscIdAsc();
    }

    public BankAssignmentRule saveRule(BankAssignmentRule input) {
        require("BANK_ACCOUNT:MANAGE");
        BankAssignmentRule rule = input.getId() == null ? BankAssignmentRule.builder().createdBy(actors.resolve("SYSTEM")).build()
                : ruleRepository.findById(input.getId()).orElseThrow(() -> new ResourceNotFoundException("Regle introuvable"));
        Pattern.compile(input.getLabelPattern(), Pattern.CASE_INSENSITIVE);
        chart.account(input.getAccountNumber());
        rule.setLabel(input.getLabel());
        rule.setLabelPattern(input.getLabelPattern());
        rule.setDirection(input.getDirection());
        rule.setMinAmount(input.getMinAmount());
        rule.setMaxAmount(input.getMaxAmount());
        rule.setAccountNumber(input.getAccountNumber());
        rule.setThirdParty(input.getThirdParty());
        rule.setPriority(input.getPriority() == null ? 100 : input.getPriority());
        rule.setActive(input.getActive() == null || input.getActive());
        return ruleRepository.save(rule);
    }

    /** Regle d'affectation proposee pour une ligne de releve. */
    @Transactional(readOnly = true)
    public Optional<BankAssignmentRule> ruleFor(BankStatementLine line) {
        return ruleRepository.findByActiveTrueOrderByPriorityAscIdAsc().stream()
                .filter(rule -> Pattern.compile(rule.getLabelPattern(), Pattern.CASE_INSENSITIVE).matcher(line.getLabel()).find())
                .filter(rule -> rule.getDirection() == null || ("IN".equals(rule.getDirection()) == (line.getAmount().signum() > 0)))
                .filter(rule -> rule.getMinAmount() == null || line.getAmount().abs().compareTo(rule.getMinAmount()) >= 0)
                .filter(rule -> rule.getMaxAmount() == null || line.getAmount().abs().compareTo(rule.getMaxAmount()) <= 0)
                .findFirst();
    }

    /**
     * Comptabiliser une ligne de releve sans ecriture : par une regle d'affectation ou un compte
     * (ecriture validee puis pointee), ou par un paiement client par virement impute a sa facture
     * (le paiement produit son ecriture d'encaissement, pointee a son arrivee par sa reference).
     */
    public Line post(String accountCode, Long lineId, PostLine input) {
        require("BANK_RECONCILIATION:MANAGE");
        BankAccount account = account(accountCode);
        BankStatementLine line = statementLineRepository.findById(lineId).orElseThrow(() -> new ResourceNotFoundException("Ligne de releve introuvable"));
        if (!line.getBankAccountCode().equals(account.getCode()) || line.getMatchingCode() != null || line.getAwaitingPayment() != null) {
            throw new BadRequestException("Cette ligne est deja pointee ou en attente de son paiement");
        }
        String actor = actors.resolve("SYSTEM");
        if (input != null && StringUtils.hasText(input.invoiceNumber())) {
            if (line.getAmount().signum() <= 0) {
                throw new BadRequestException("Seul un credit du releve devient un paiement client");
            }
            var paid = paymentService.payInvoice(input.invoiceNumber().trim(), new PayInvoiceRequest(PaymentMethod.BANK_TRANSFER, line.getAmount(), null,
                    null, StringUtils.hasText(line.getReference()) ? line.getReference() : "RELEVE-" + line.getId(), actor, "BANKLINE-" + line.getId(), null));
            line.setAwaitingPayment(paid.transaction().transactionNumber());
            statementLineRepository.save(line);
            return view(line);
        }
        String counter;
        String thirdParty = input == null ? null : input.thirdParty();
        if (input != null && StringUtils.hasText(input.accountNumber())) {
            counter = input.accountNumber().trim();
        } else if (input != null && input.ruleId() != null) {
            BankAssignmentRule rule = ruleRepository.findById(input.ruleId()).orElseThrow(() -> new ResourceNotFoundException("Regle introuvable"));
            counter = rule.getAccountNumber();
            thirdParty = thirdParty == null ? rule.getThirdParty() : thirdParty;
        } else {
            BankAssignmentRule rule = ruleFor(line).orElseThrow(() -> new BadRequestException(
                    "Aucune regle ne s'applique : indiquez le compte, ou le numero de facture d'un virement client"));
            counter = rule.getAccountNumber();
            thirdParty = rule.getThirdParty();
        }
        BigDecimal amount = line.getAmount().abs();
        boolean in = line.getAmount().signum() > 0;
        String label = input != null && StringUtils.hasText(input.label()) ? input.label() : line.getLabel();
        List<AccountingRequests.EntryLine> lines = List.of(
                new AccountingRequests.EntryLine(account.getAccountNumber(), null, label, in ? amount : null, in ? null : amount, null, null, null, null,
                        null, null, line.getReference()),
                new AccountingRequests.EntryLine(counter, thirdParty, label, in ? null : amount, in ? amount : null, null, null, null, null, null, null,
                        line.getReference()));
        AccEntry entry = entries.post(new AccountingEntryService.AutoPost(account.getJournalCode(), line.getOperationDate(), null, label,
                EntryOrigin.PLATFORM, "BANK_STATEMENT_LINE", String.valueOf(line.getId()), lines, actor, true, null));
        if (entry.getStatus() != EntryStatus.VALIDATED) {
            throw new BadRequestException("L'ecriture n'a pas pu etre validee : " + entry.getPendingDetail());
        }
        line.setPostedEntry(entry.getEntryNumber());
        statementLineRepository.save(line);
        AccEntryLine bankLine = entryLineRepository.findByEntryIdOrderByLineNoAsc(entry.getId()).stream()
                .filter(l -> l.getAccountNumber().equals(account.getAccountNumber())).findFirst().orElseThrow();
        match(account, List.of(line.getId()), List.of(bankLine.getId()), "POSTED", actor);
        return view(statementLineRepository.findById(line.getId()).orElseThrow());
    }

    // ------------------------------------------------------------------ rapprochement

    /** Etat de rapprochement a une date (brouillon recalcule a chaque preparation). */
    public Reconciliation prepare(String accountCode, LocalDate date) {
        require("BANK_RECONCILIATION:MANAGE");
        BankAccount account = account(accountCode);
        Reconciliation computed = compute(account, date);
        BankReconciliation saved = reconciliationRepository.save(BankReconciliation.builder().bankAccountCode(account.getCode())
                .reconciliationDate(date).statementBalance(computed.statementBalance()).bookBalance(computed.bookBalance())
                .outstanding(computed.bookOutstanding().size() + " ecriture(s) et " + computed.statementOutstanding().size() + " ligne(s) de releve en suspens")
                .difference(computed.difference()).preparedBy(actors.resolve("SYSTEM")).build());
        return withRecord(computed, saved);
    }

    Reconciliation compute(BankAccount account, LocalDate date) {
        List<BankStatementLine> all = statementLineRepository.findByBankAccountCodeOrderByOperationDateAsc(account.getCode()).stream()
                .filter(line -> !line.getOperationDate().isAfter(date))
                .filter(line -> statementRepository.findById(line.getStatementId()).map(s -> "IMPORTED".equals(s.getStatus())).orElse(false))
                .toList();
        BigDecimal statementBalance = account.getOpeningBalance().add(all.stream().map(BankStatementLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal book = new JdbcTemplate(dataSource).queryForObject("SELECT COALESCE(SUM(l.debit - l.credit), 0) FROM acc_entry_line l JOIN acc_entry e "
                + "ON e.id = l.entry_id WHERE e.status IN ('VALIDATED', 'REVERSED') AND l.account_number = ? AND e.entry_date <= ?", BigDecimal.class,
                account.getAccountNumber(), java.sql.Date.valueOf(date));
        // Le solde d'ouverture du releve doit figurer en comptabilite (a-nouveaux) : il n'est pas ajoute ici.
        BigDecimal bookBalance = book == null ? BigDecimal.ZERO : book;
        List<BookLine> bookOutstanding = openBookLines(account, date);
        List<Line> statementOutstanding = all.stream().filter(line -> line.getMatchingCode() == null).map(this::view).toList();
        BigDecimal reconciled = statementBalance.add(bookOutstanding.stream().map(BookLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .subtract(statementOutstanding.stream().map(Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add));
        return new Reconciliation(null, account.getCode(), date, scale(statementBalance), scale(bookBalance), bookOutstanding, statementOutstanding,
                scale(reconciled), scale(reconciled.subtract(bookBalance)), "DRAFT", null, null, null, null);
    }

    /** Validation par une autre personne, ecart nul : numero RAP, pointages verrouilles. */
    public Reconciliation validate(Long id) {
        require("BANK_RECONCILIATION:VALIDATE");
        BankReconciliation record = reconciliationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Rapprochement introuvable"));
        if (!"DRAFT".equals(record.getStatus())) {
            throw new BadRequestException("Ce rapprochement est deja valide");
        }
        String actor = actors.resolve("SYSTEM");
        if (CashActorResolver.samePerson(actor, record.getPreparedBy())) {
            throw new ForbiddenException("Le rapprochement est valide par une autre personne que celle qui l'a prepare");
        }
        BankAccount account = account(record.getBankAccountCode());
        Reconciliation computed = compute(account, record.getReconciliationDate());
        if (computed.difference().signum() != 0) {
            throw new BadRequestException("Ecart de " + plain(computed.difference()) + " XAF : un rapprochement ne se valide qu'avec un ecart nul");
        }
        VoucherService.Issued issued = vouchers.issueAndPrint("RAP", "BANK_RECONCILIATION", String.valueOf(record.getId()), computed.statementBalance(),
                account.getCode() + " au " + record.getReconciliationDate(), actor);
        record.setNumber(issued.number());
        record.setStatus("VALIDATED");
        record.setValidatedBy(actor);
        record.setValidatedAt(Instant.now());
        record.setStatementBalance(computed.statementBalance());
        record.setBookBalance(computed.bookBalance());
        record.setDifference(BigDecimal.ZERO);
        reconciliationRepository.save(record);
        matchingRepository.findByBankAccountCodeAndUnmatchedAtIsNull(account.getCode()).forEach(matching -> {
            matching.setLocked(true);
            matchingRepository.save(matching);
        });
        return withRecord(computed, record);
    }

    @Transactional(readOnly = true)
    public List<BankReconciliation> reconciliations(String accountCode) {
        return reconciliationRepository.findByBankAccountCodeOrderByReconciliationDateDesc(AnalyticService.code(accountCode));
    }

    /** Pour le verrouillage d'une periode : chaque compte suivi a un rapprochement valide a la fin de la periode. */
    @Transactional(readOnly = true)
    public List<String> missingReconciliations(LocalDate periodEnd) {
        List<String> missing = new ArrayList<>();
        for (BankAccount account : accountRepository.findAllByOrderByCodeAsc()) {
            if (!Boolean.TRUE.equals(account.getActive()) || statementRepository.findByBankAccountCodeAndStatusOrderByPeriodToDesc(account.getCode(), "IMPORTED").isEmpty()) {
                continue;
            }
            boolean ok = reconciliationRepository.findByBankAccountCodeOrderByReconciliationDateDesc(account.getCode()).stream()
                    .anyMatch(r -> "VALIDATED".equals(r.getStatus()) && !r.getReconciliationDate().isBefore(periodEnd));
            if (!ok) {
                missing.add(account.getCode());
            }
        }
        return missing;
    }

    public record Dashboard(String code, String label, String accountNumber, BigDecimal statementBalance, BigDecimal bookBalance, int bookOutstanding,
                            int statementOutstanding, Long oldestOutstandingDays, LocalDate lastStatement, LocalDate lastReconciliation) {
    }

    @Transactional(readOnly = true)
    public List<Dashboard> dashboard() {
        LocalDate today = LocalDate.now();
        List<Dashboard> rows = new ArrayList<>();
        for (BankAccount account : accountRepository.findAllByOrderByCodeAsc()) {
            Reconciliation now = compute(account, today);
            Long oldest = now.bookOutstanding().stream().map(BookLine::date).min(LocalDate::compareTo)
                    .map(date -> ChronoUnit.DAYS.between(date, today)).orElse(null);
            rows.add(new Dashboard(account.getCode(), account.getLabel(), account.getAccountNumber(), now.statementBalance(), now.bookBalance(),
                    now.bookOutstanding().size(), now.statementOutstanding().size(), oldest,
                    statementRepository.findByBankAccountCodeAndStatusOrderByPeriodToDesc(account.getCode(), "IMPORTED").stream().findFirst()
                            .map(BankStatement::getPeriodTo).orElse(null),
                    reconciliationRepository.findByBankAccountCodeOrderByReconciliationDateDesc(account.getCode()).stream()
                            .filter(r -> "VALIDATED".equals(r.getStatus())).findFirst().map(BankReconciliation::getReconciliationDate).orElse(null)));
        }
        return rows;
    }

    // ------------------------------------------------------------------ outils

    private Reconciliation withRecord(Reconciliation computed, BankReconciliation record) {
        return new Reconciliation(record.getNumber(), computed.bankAccountCode(), computed.date(), computed.statementBalance(), computed.bookBalance(),
                computed.bookOutstanding(), computed.statementOutstanding(), computed.reconciledBalance(), computed.difference(), record.getStatus(),
                record.getPreparedBy(), record.getValidatedBy(), record.getValidatedAt(), record.getId());
    }

    private Line view(BankStatementLine line) {
        String number = statementRepository.findById(line.getStatementId()).map(BankStatement::getNumber).orElse(null);
        return new Line(line.getId(), number, line.getOperationDate(), line.getValueDate(), line.getLabel(), line.getReference(), line.getAmount(),
                line.getMatchingCode(), line.getAwaitingPayment(), line.getPostedEntry());
    }

    BankAccount account(String code) {
        return accountRepository.findByCode(AnalyticService.code(code)).orElseThrow(() -> new ResourceNotFoundException("Compte bancaire introuvable : " + code));
    }

    private void require(String authority) {
        if (!actors.hasAuthority(authority)) {
            throw new ForbiddenException("Cette action demande " + authority);
        }
    }

    private static String join(List<Long> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private static List<Long> ids(String value) {
        return Arrays.stream(value.split(",")).filter(StringUtils::hasText).map(Long::valueOf).toList();
    }

    private static long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value.trim());
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    static BigDecimal scale(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(4, RoundingMode.HALF_UP);
    }

    static String plain(BigDecimal value) {
        return value == null ? "0" : value.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }
}
