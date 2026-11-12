package com.sni.bokaticowork.features.accounting.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.dto.AccountingResponses;
import com.sni.bokaticowork.features.accounting.enums.AccountSide;
import com.sni.bokaticowork.features.accounting.model.*;
import com.sni.bokaticowork.features.accounting.repository.*;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Import du journal de paie (lot F17) : modele d'import parametrable, correspondance des rubriques
 * vers les comptes, controles (equilibre par salarie, rubriques et comptes connus, avances retenues
 * couvertes), ecriture au journal {@code PA} avec ventilation par section, lettrage des avances
 * retenues sur {@code 421}, paiement des salaires lettre sur {@code 422}.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class PayrollService {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final AccPayrollTemplateRepository templates;
    private final AccPayrollRubricRepository rubrics;
    private final AccPayrollEmployeeRepository employees;
    private final AccPayrollImportRepository imports;
    private final AccPayrollLineRepository lines;
    private final AccEntryLineRepository entryLines;
    private final AccEntryRepository entryRepository;
    private final AccountingLetteringService lettering;
    private final LoanService loans;
    private final AccountingSettings settings;
    private final CashActorResolver actors;

    // ------------------------------------------------------------------ parametrage

    @Transactional(readOnly = true)
    public List<AccPayrollTemplate> templates() {
        return templates.findAllByOrderByCodeAsc();
    }

    public AccPayrollTemplate saveTemplate(AccPayrollTemplate template) {
        require("PAYROLL_SETTINGS:MANAGE");
        if (!StringUtils.hasText(template.getCode()) || !StringUtils.hasText(template.getEmployeeColumn()) || !StringUtils.hasText(template.getRubricColumn())
                || !StringUtils.hasText(template.getAmountColumn())) {
            throw new BadRequestException("Code et colonnes du matricule, de la rubrique et du montant obligatoires");
        }
        template.setCode(template.getCode().trim().toUpperCase(Locale.ROOT));
        if (template.getLabel() == null) {
            template.setLabel(template.getCode());
        }
        return templates.save(template);
    }

    @Transactional(readOnly = true)
    public List<AccPayrollRubric> rubrics() {
        return rubrics.findAllByOrderByCodeAsc();
    }

    public AccPayrollRubric saveRubric(AccPayrollRubric rubric) {
        require("PAYROLL_SETTINGS:MANAGE");
        if (!StringUtils.hasText(rubric.getCode()) || !StringUtils.hasText(rubric.getKind())) {
            throw new BadRequestException("Code et nature de la rubrique obligatoires");
        }
        rubric.setCode(normalize(rubric.getCode()));
        rubric.setKind(rubric.getKind().trim().toUpperCase(Locale.ROOT));
        if (!"INFO".equals(rubric.getKind()) && !StringUtils.hasText(rubric.getAccount())) {
            throw new BadRequestException("La rubrique " + rubric.getCode() + " doit avoir un compte");
        }
        if ("EMPLOYER_CHARGE".equals(rubric.getKind()) && !StringUtils.hasText(rubric.getCounterAccount())) {
            throw new BadRequestException("Une charge patronale a un compte de charge et un compte d'organisme");
        }
        if (rubric.getLabel() == null) {
            rubric.setLabel(rubric.getCode());
        }
        return rubrics.save(rubric);
    }

    @Transactional(readOnly = true)
    public List<AccPayrollEmployee> employees() {
        return employees.findAllByOrderByMatriculeAsc();
    }

    public AccPayrollEmployee saveEmployee(AccPayrollEmployee employee) {
        require("PAYROLL_SETTINGS:MANAGE");
        if (!StringUtils.hasText(employee.getMatricule()) || !StringUtils.hasText(employee.getThirdPartyCode())) {
            throw new BadRequestException("Matricule et tiers comptable (adresse du salarie) obligatoires");
        }
        employee.setMatricule(employee.getMatricule().trim());
        if (employee.getFullName() == null) {
            employee.setFullName(employee.getMatricule());
        }
        return employees.save(employee);
    }

    // ------------------------------------------------------------------ import

    public record ImportView(AccPayrollImport payroll, List<String> errors, List<String> warnings, List<EmployeeTotals> employees) {
    }

    public record EmployeeTotals(String matricule, String name, String thirdPartyCode, String section, BigDecimal gross, BigDecimal deductions,
                                 BigDecimal advances, BigDecimal net, BigDecimal employerCharges, BigDecimal difference) {
    }

    @Transactional(readOnly = true)
    public List<AccPayrollImport> list() {
        return imports.findTop36ByOrderByImportedAtDesc();
    }

    @Transactional(readOnly = true)
    public ImportView view(Long id) {
        AccPayrollImport payroll = payroll(id);
        return new ImportView(payroll, read(payroll.getErrorsJson()), read(payroll.getWarningsJson()), totals(lines.findAllByImportIdOrderByLineNoAsc(id)));
    }

    @Transactional(readOnly = true)
    public List<AccPayrollLine> lines(Long id) {
        return lines.findAllByImportIdOrderByLineNoAsc(payroll(id).getId());
    }

    /**
     * Import d'un journal de paie pour un mois. Un reimport remplace le brouillon du mois, jamais
     * un journal comptabilise.
     */
    public ImportView importFile(String period, String templateCode, byte[] content, String fileName) {
        require("PAYROLL:IMPORT");
        YearMonth month;
        try {
            month = YearMonth.parse(period.trim());
        } catch (RuntimeException exception) {
            throw new BadRequestException("Periode attendue au format AAAA-MM");
        }
        AccPayrollTemplate template = templates.findById(StringUtils.hasText(templateCode) ? templateCode.trim().toUpperCase(Locale.ROOT) : "STANDARD")
                .orElseThrow(() -> new BadRequestException("Modele d'import inconnu : " + templateCode));
        for (AccPayrollImport existing : imports.findAllByPeriodCodeAndStatusNot(month.toString(), "REPLACED")) {
            if ("POSTED".equals(existing.getStatus())) {
                throw new BadRequestException("La paie " + month + " est deja comptabilisee (" + existing.getEntryNumber() + ")");
            }
            existing.setStatus("REPLACED");
            imports.saveAndFlush(existing);
        }
        List<AccPayrollLine> parsed = parse(template, content);
        if (parsed.isEmpty()) {
            throw new BadRequestException("Aucune ligne lue dans le fichier");
        }
        AccPayrollImport payroll = imports.save(AccPayrollImport.builder().periodCode(month.toString()).templateCode(template.getCode()).fileName(fileName)
                .importedBy(actors.resolve("SYSTEM")).build());
        for (AccPayrollLine line : parsed) {
            line.setImportId(payroll.getId());
            lines.save(line);
        }
        check(payroll, parsed);
        return view(payroll.getId());
    }

    private List<AccPayrollLine> parse(AccPayrollTemplate template, byte[] content) {
        String text = new String(content, Charset.forName(template.getEncoding() == null ? "UTF-8" : template.getEncoding()));
        if (text.startsWith("﻿")) {
            text = text.substring(1);
        }
        String[] rows = text.split("\\r?\\n");
        String separator = template.getSeparator() == null || template.getSeparator().isEmpty() ? ";" : template.getSeparator();
        String[] header = template.getHeaderRows() > 0 && rows.length > 0 ? split(rows[0], separator) : new String[0];
        int employee = column(header, template.getEmployeeColumn(), true);
        int name = column(header, template.getNameColumn(), false);
        int rubric = column(header, template.getRubricColumn(), true);
        int amount = column(header, template.getAmountColumn(), true);
        int section = column(header, template.getSectionColumn(), false);
        List<AccPayrollLine> result = new ArrayList<>();
        for (int i = Math.max(0, template.getHeaderRows()); i < rows.length; i++) {
            if (rows[i].isBlank()) {
                continue;
            }
            String[] cells = split(rows[i], separator);
            String value = cell(cells, amount);
            if (!StringUtils.hasText(cell(cells, employee)) || !StringUtils.hasText(cell(cells, rubric)) || !StringUtils.hasText(value)) {
                continue;
            }
            BigDecimal number;
            try {
                String clean = value.replace(" ", "").replace(" ", "");
                clean = ",".equals(template.getDecimalSeparator()) ? clean.replace(".", "").replace(",", ".") : clean.replace(",", "");
                number = new BigDecimal(clean);
            } catch (RuntimeException exception) {
                throw new BadRequestException("Ligne " + (i + 1) + " : montant illisible « " + value + " »");
            }
            result.add(AccPayrollLine.builder().lineNo(i + 1).matricule(cell(cells, employee)).employeeName(cell(cells, name))
                    .rubricCode(normalize(cell(cells, rubric))).amount(LoanService.money(number)).sectionCode(cell(cells, section)).build());
        }
        return result;
    }

    /** Controles : rubriques et comptes connus, equilibre par salarie, avances retenues couvertes, sections. */
    private void check(AccPayrollImport payroll, List<AccPayrollLine> parsed) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Map<String, AccPayrollRubric> known = new HashMap<>();
        rubrics.findAll().forEach(rubric -> known.put(rubric.getCode(), rubric));
        Set<String> unknown = new TreeSet<>();
        for (AccPayrollLine line : parsed) {
            AccPayrollRubric rubric = known.get(line.getRubricCode());
            if (rubric == null || !Boolean.TRUE.equals(rubric.getActive())) {
                unknown.add(line.getRubricCode());
            }
        }
        unknown.forEach(code -> errors.add("Rubrique sans correspondance de compte : " + code));
        List<EmployeeTotals> totals = totals(parsed);
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal employer = BigDecimal.ZERO;
        BigDecimal deductions = BigDecimal.ZERO;
        BigDecimal advances = BigDecimal.ZERO;
        BigDecimal net = BigDecimal.ZERO;
        boolean sectionRequired = "true".equalsIgnoreCase(settings.text("accounting.payroll.section-required", "false"));
        for (EmployeeTotals employee : totals) {
            if (employee.difference().signum() != 0) {
                errors.add("Salarie " + employee.matricule() + " : le brut (" + LoanService.plain(employee.gross()) + ") n'egale pas net + retenues + avances ("
                        + LoanService.plain(employee.net().add(employee.deductions()).add(employee.advances())) + ")");
            }
            if (employees.findById(employee.matricule()).isEmpty()) {
                warnings.add("Salarie " + employee.matricule() + " absent de la liste des salaries : tiers comptable = matricule");
            }
            if (!StringUtils.hasText(employee.section())) {
                (sectionRequired ? errors : warnings).add("Salarie " + employee.matricule() + " sans section analytique");
            }
            if (employee.advances().signum() > 0) {
                BigDecimal open = openAdvances(employee.thirdPartyCode());
                if (employee.advances().compareTo(open) > 0) {
                    errors.add("Salarie " + employee.matricule() + " : retenue d'avance (" + LoanService.plain(employee.advances())
                            + ") superieure aux avances ouvertes en 421 (" + LoanService.plain(open) + ")");
                }
            }
            gross = gross.add(employee.gross());
            employer = employer.add(employee.employerCharges());
            deductions = deductions.add(employee.deductions());
            advances = advances.add(employee.advances());
            net = net.add(employee.net());
        }
        payroll.setGrossTotal(gross);
        payroll.setEmployerTotal(employer);
        payroll.setDeductionTotal(deductions);
        payroll.setAdvanceTotal(advances);
        payroll.setNetTotal(net);
        payroll.setEmployeeCount(totals.size());
        payroll.setErrorsJson(write(errors));
        payroll.setWarningsJson(write(warnings));
        imports.save(payroll);
    }

    private List<EmployeeTotals> totals(List<AccPayrollLine> parsed) {
        Map<String, AccPayrollRubric> known = new HashMap<>();
        rubrics.findAll().forEach(rubric -> known.put(rubric.getCode(), rubric));
        Map<String, BigDecimal[]> sums = new LinkedHashMap<>();
        Map<String, String> names = new HashMap<>();
        Map<String, String> sections = new HashMap<>();
        for (AccPayrollLine line : parsed) {
            BigDecimal[] values = sums.computeIfAbsent(line.getMatricule(), ignored -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO});
            if (StringUtils.hasText(line.getEmployeeName())) {
                names.put(line.getMatricule(), line.getEmployeeName());
            }
            if (StringUtils.hasText(line.getSectionCode())) {
                sections.put(line.getMatricule(), line.getSectionCode());
            }
            AccPayrollRubric rubric = known.get(line.getRubricCode());
            if (rubric == null) {
                continue;
            }
            int index = switch (rubric.getKind()) {
                case "GROSS" -> 0;
                case "DEDUCTION" -> 1;
                case "ADVANCE" -> 2;
                case "NET" -> 3;
                case "EMPLOYER_CHARGE" -> 4;
                default -> -1;
            };
            if (index >= 0) {
                values[index] = values[index].add(line.getAmount());
            }
        }
        List<EmployeeTotals> result = new ArrayList<>();
        sums.forEach((matricule, values) -> {
            Optional<AccPayrollEmployee> employee = employees.findById(matricule);
            String section = sections.getOrDefault(matricule, employee.map(AccPayrollEmployee::getSectionCode).orElse(null));
            result.add(new EmployeeTotals(matricule, names.getOrDefault(matricule, employee.map(AccPayrollEmployee::getFullName).orElse(matricule)),
                    employee.map(AccPayrollEmployee::getThirdPartyCode).orElse(matricule), section, values[0], values[1], values[2], values[3], values[4],
                    values[0].subtract(values[1]).subtract(values[2]).subtract(values[3])));
        });
        return result;
    }

    // ------------------------------------------------------------------ comptabilisation

    /**
     * Ecriture du journal de paie au journal {@code PA}, datee du dernier jour du mois : D
     * remunerations et charges patronales par section ; C organismes, Etat, avances (421) et net du
     * (422) par salarie. Les avances retenues sont lettrees avec les avances ouvertes, les plus
     * anciennes d'abord.
     */
    public ImportView post(Long id) {
        require("PAYROLL:POST");
        AccPayrollImport payroll = payroll(id);
        if (!"DRAFT".equals(payroll.getStatus())) {
            throw new BadRequestException("Seul un brouillon se comptabilise");
        }
        List<String> errors = read(payroll.getErrorsJson());
        if (!errors.isEmpty()) {
            throw new BadRequestException("Le journal de paie a " + errors.size() + " erreur(s) : " + errors.get(0));
        }
        Map<String, AccPayrollRubric> known = new HashMap<>();
        rubrics.findAll().forEach(rubric -> known.put(rubric.getCode(), rubric));
        List<AccPayrollLine> payrollLines = lines.findAllByImportIdOrderByLineNoAsc(id);
        Map<String, EmployeeTotals> byEmployee = new HashMap<>();
        totals(payrollLines).forEach(item -> byEmployee.put(item.matricule(), item));
        // Agregation : cle (compte, tiers, section, sens) -> montant.
        Map<List<String>, BigDecimal> debit = new LinkedHashMap<>();
        Map<List<String>, BigDecimal> credit = new LinkedHashMap<>();
        for (AccPayrollLine line : payrollLines) {
            AccPayrollRubric rubric = known.get(line.getRubricCode());
            EmployeeTotals employee = byEmployee.get(line.getMatricule());
            if (rubric == null || "INFO".equals(rubric.getKind()) || line.getAmount().signum() == 0) {
                continue;
            }
            String section = employee.section();
            String tp = employee.thirdPartyCode();
            switch (rubric.getKind()) {
                case "GROSS" -> debit.merge(Arrays.asList(rubric.getAccount(), null, section), line.getAmount(), BigDecimal::add);
                case "EMPLOYER_CHARGE" -> {
                    debit.merge(Arrays.asList(rubric.getAccount(), null, section), line.getAmount(), BigDecimal::add);
                    credit.merge(Arrays.asList(rubric.getCounterAccount(), null, null), line.getAmount(), BigDecimal::add);
                }
                case "DEDUCTION" -> credit.merge(Arrays.asList(rubric.getAccount(), null, null), line.getAmount(), BigDecimal::add);
                default -> credit.merge(Arrays.asList(rubric.getAccount(), tp, null), line.getAmount(), BigDecimal::add);
            }
        }
        LocalDate date = YearMonth.parse(payroll.getPeriodCode()).atEndOfMonth();
        String label = "Paie " + payroll.getPeriodCode();
        List<AccountingRequests.EntryLine> entry = new ArrayList<>();
        debit.forEach((key, amount) -> entry.add(signed(key, amount, true, label, payroll.getPeriodCode())));
        credit.forEach((key, amount) -> entry.add(signed(key, amount, false, label, payroll.getPeriodCode())));
        AccEntry posted = loans.post(settings.text("accounting.payroll.journal", "PA"), date, label, "PAYROLL", payroll.getPeriodCode(), entry);
        payroll.setStatus("POSTED");
        payroll.setEntryNumber(posted.getEntryNumber());
        payroll.setPostedBy(actors.resolve("SYSTEM"));
        payroll.setPostedAt(Instant.now());
        imports.save(payroll);
        letterAdvances(posted, byEmployee.values());
        return view(id);
    }

    /** Une ligne en debit ou credit selon le signe du montant agrege. */
    private AccountingRequests.EntryLine signed(List<String> key, BigDecimal amount, boolean debitSide, String label, String period) {
        boolean debit = amount.signum() >= 0 == debitSide;
        BigDecimal value = amount.abs();
        String reference = key.get(1) == null ? period : "PAIE-" + period;
        return loans.line(key.get(0), key.get(1), label, debit ? value : null, debit ? null : value, reference, key.get(2));
    }

    /** Lettrage de chaque retenue d'avance (credit 421) avec les avances ouvertes du salarie, les plus anciennes d'abord. */
    private void letterAdvances(AccEntry posted, Collection<EmployeeTotals> employeesTotals) {
        String advanceAccount = rubrics.findAll().stream().filter(rubric -> "ADVANCE".equals(rubric.getKind())).map(AccPayrollRubric::getAccount)
                .findFirst().orElse("421");
        List<AccEntryLine> postedLines = entryLines.findByEntryIdOrderByLineNoAsc(posted.getId());
        for (EmployeeTotals employee : employeesTotals) {
            if (employee.advances().signum() <= 0) {
                continue;
            }
            AccEntryLine retained = postedLines.stream().filter(line -> advanceAccount.equals(line.getAccountNumber())
                    && employee.thirdPartyCode().equals(line.getThirdPartyCode()) && line.getCredit().signum() > 0).findFirst().orElse(null);
            if (retained == null) {
                continue;
            }
            BigDecimal left = retained.getCredit();
            List<AccountingRequests.LetterLine> letter = new ArrayList<>();
            letter.add(new AccountingRequests.LetterLine(retained.getId(), left));
            List<AccountingResponses.OpenLine> open = lettering.openLines(advanceAccount, employee.thirdPartyCode()).stream()
                    .filter(line -> line.side() == AccountSide.DEBIT).sorted(Comparator.comparing(AccountingResponses.OpenLine::entryDate)).toList();
            for (AccountingResponses.OpenLine line : open) {
                if (left.signum() <= 0) {
                    break;
                }
                BigDecimal take = line.remaining().min(left);
                letter.add(new AccountingRequests.LetterLine(line.lineId(), take));
                left = left.subtract(take);
            }
            if (letter.size() > 1 && left.signum() == 0) {
                lettering.letter(new AccountingRequests.Letter(advanceAccount, employee.thirdPartyCode(), letter, false, false));
            }
        }
    }

    public record PaymentRequest(String treasuryAccount, String journalCode, LocalDate date, List<String> matricules) {
    }

    /**
     * Paiement des salaires par virement ou en especes : D 422 par salarie ; C tresorerie, lettre avec
     * le net du. Sans liste de matricules, tous les nets restant dus du mois.
     */
    public ImportView pay(Long id, PaymentRequest request) {
        require("PAYROLL:POST");
        AccPayrollImport payroll = payroll(id);
        if (!"POSTED".equals(payroll.getStatus())) {
            throw new BadRequestException("La paie doit etre comptabilisee avant son paiement");
        }
        if (request == null || !StringUtils.hasText(request.treasuryAccount()) || !StringUtils.hasText(request.journalCode())) {
            throw new BadRequestException("Compte et journal de tresorerie obligatoires");
        }
        String netAccount = rubrics.findAll().stream().filter(rubric -> "NET".equals(rubric.getKind())).map(AccPayrollRubric::getAccount).findFirst().orElse("422");
        AccEntry payrollEntry = entryRepository.findByEntryNumber(payroll.getEntryNumber()).orElseThrow();
        Set<String> wanted = request.matricules() == null || request.matricules().isEmpty() ? null : new HashSet<>(request.matricules());
        Map<String, EmployeeTotals> byThirdParty = new HashMap<>();
        totals(lines.findAllByImportIdOrderByLineNoAsc(id)).forEach(item -> byThirdParty.put(item.thirdPartyCode(), item));
        List<AccountingRequests.EntryLine> entry = new ArrayList<>();
        List<AccEntryLine> dueLines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        String label = "Paiement des salaires " + payroll.getPeriodCode();
        for (AccountingResponses.OpenLine open : lettering.openLines(netAccount, null)) {
            if (open.side() != AccountSide.CREDIT || !payroll.getEntryNumber().equals(open.entryNumber())) {
                continue;
            }
            EmployeeTotals employee = byThirdParty.get(open.thirdPartyCode());
            if (wanted != null && (employee == null || !wanted.contains(employee.matricule()))) {
                continue;
            }
            entry.add(loans.line(netAccount, open.thirdPartyCode(), label, open.remaining(), null, "PAIE-" + payroll.getPeriodCode(), null));
            total = total.add(open.remaining());
            dueLines.add(entryLines.findById(open.lineId()).orElseThrow());
        }
        if (total.signum() == 0) {
            throw new BadRequestException("Aucun net restant du pour cette paie");
        }
        entry.add(loans.line(request.treasuryAccount().trim(), null, label, null, total, "PAIE-" + payroll.getPeriodCode(), null));
        AccEntry payment = loans.post(request.journalCode().trim(), request.date() == null ? LocalDate.now() : request.date(), label, "PAYROLL_PAYMENT",
                payroll.getPeriodCode(), entry);
        List<AccEntryLine> paymentLines = entryLines.findByEntryIdOrderByLineNoAsc(payment.getId());
        for (AccEntryLine due : dueLines) {
            paymentLines.stream().filter(line -> netAccount.equals(line.getAccountNumber()) && Objects.equals(due.getThirdPartyCode(), line.getThirdPartyCode()))
                    .findFirst().ifPresent(paid -> lettering.letter(new AccountingRequests.Letter(netAccount, due.getThirdPartyCode(), List.of(
                            new AccountingRequests.LetterLine(due.getId(), null), new AccountingRequests.LetterLine(paid.getId(), null)), false, false)));
        }
        payroll.setPaidTotal(payroll.getPaidTotal().add(total));
        imports.save(payroll);
        return view(id);
    }

    // ------------------------------------------------------------------ outils

    private BigDecimal openAdvances(String thirdParty) {
        String account = rubrics.findAll().stream().filter(rubric -> "ADVANCE".equals(rubric.getKind())).map(AccPayrollRubric::getAccount).findFirst().orElse("421");
        return lettering.openLines(account, thirdParty).stream().map(line -> line.side() == AccountSide.DEBIT ? line.remaining() : line.remaining().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private AccPayrollImport payroll(Long id) {
        return imports.findById(id).orElseThrow(() -> new ResourceNotFoundException("Journal de paie introuvable"));
    }

    private void require(String authority) {
        if (!actors.hasAuthority(authority)) {
            throw new ForbiddenException("Action reservee : " + authority);
        }
    }

    private static String normalize(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_|_$", "");
    }

    private static String[] split(String row, String separator) {
        String[] cells = row.split(java.util.regex.Pattern.quote(separator), -1);
        for (int i = 0; i < cells.length; i++) {
            cells[i] = cells[i].trim().replaceAll("^\"|\"$", "");
        }
        return cells;
    }

    /** Colonne par nom d'en-tete ou par numero (1 = premiere colonne). */
    private static int column(String[] header, String spec, boolean required) {
        if (!StringUtils.hasText(spec)) {
            if (required) {
                throw new BadRequestException("Colonne obligatoire non indiquee dans le modele");
            }
            return -1;
        }
        if (spec.trim().matches("\\d+")) {
            return Integer.parseInt(spec.trim()) - 1;
        }
        for (int i = 0; i < header.length; i++) {
            if (header[i].equalsIgnoreCase(spec.trim())) {
                return i;
            }
        }
        if (required) {
            throw new BadRequestException("Colonne « " + spec + " » absente de l'en-tete du fichier");
        }
        return -1;
    }

    private static String cell(String[] cells, int index) {
        return index < 0 || index >= cells.length || cells[index].isBlank() ? null : cells[index];
    }

    private static String write(List<String> values) {
        try {
            return JSON.writeValueAsString(values);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static List<String> read(String json) {
        if (json == null) {
            return List.of();
        }
        try {
            return Arrays.asList(JSON.readValue(json, String[].class));
        } catch (Exception exception) {
            return List.of(json);
        }
    }
}
