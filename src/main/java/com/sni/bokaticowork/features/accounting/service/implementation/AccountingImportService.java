package com.sni.bokaticowork.features.accounting.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.dto.AccountingResponses;
import com.sni.bokaticowork.features.accounting.enums.EntryOrigin;
import com.sni.bokaticowork.features.accounting.enums.EntryStatus;
import com.sni.bokaticowork.features.accounting.model.AccEntry;
import com.sni.bokaticowork.features.accounting.model.AccExportMapping;
import com.sni.bokaticowork.features.accounting.model.AccFiscalYear;
import com.sni.bokaticowork.features.accounting.repository.AccExportMappingRepository;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Outils de reprise et de bascule (lot F14, plan, section 17) : import d'ecritures (constitution,
 * reprise d'un autre logiciel) en brouillard ou validees ; import des soldes d'ouverture dans le
 * journal des a-nouveaux, detailles par piece pour les comptes lettrables ; comparaison avec la
 * balance de l'outil de l'expert-comptable pendant la double tenue.
 *
 * <p>Fichiers CSV a point-virgule (ou virgule), avec en-tete. Dates AAAA-MM-JJ ou JJ/MM/AAAA,
 * montants a point ou a virgule. Encodage UTF-8, ou Windows-1252 si le fichier n'est pas en UTF-8.</p>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AccountingImportService {

    public static final String RUN = "ACCOUNTING_IMPORT:RUN";

    private final AccountingEntryService entries;
    private final FinancialStatementService statements;
    private final AccExportMappingRepository mappings;
    private final AccountingSettings settings;
    private final CashActorResolver actors;

    public record ImportResult(int entries, int lines, int validated, List<String> created, List<String> errors) {
    }

    // ================================================================== ecritures

    /**
     * Ecritures : colonnes {@code journal;date;piece;compte;tiers;libelle;debit;credit;reference;echeance}.
     * Une piece par couple journal et numero de piece ; chacune se cree en brouillard, et se valide
     * si {@code validate} est vrai, dans l'ordre des dates. Chaque piece a sa propre transaction : une
     * piece en erreur n'arrete pas le fichier et n'annule pas les autres.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public ImportResult importEntries(byte[] file, boolean validate) {
        statements.require(RUN);
        List<Map<String, String>> rows = rows(file);
        Map<String, List<Map<String, String>>> groups = new LinkedHashMap<>();
        for (Map<String, String> row : rows) {
            groups.computeIfAbsent(required(row, "journal") + "|" + required(row, "piece"), key -> new ArrayList<>()).add(row);
        }
        List<Map.Entry<String, List<Map<String, String>>>> ordered = new ArrayList<>(groups.entrySet());
        ordered.sort(java.util.Comparator.comparing(group -> date(group.getValue().get(0).get("date"))));
        List<String> created = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int lines = 0;
        int validated = 0;
        for (Map.Entry<String, List<Map<String, String>>> group : ordered) {
            Map<String, String> head = group.getValue().get(0);
            try {
                List<AccountingRequests.EntryLine> entryLines = group.getValue().stream().map(AccountingImportService::line).toList();
                AccountingResponses.Entry entry = entries.create(new AccountingRequests.Entry(required(head, "journal"), date(head.get("date")), null,
                        StringUtils.hasText(head.get("libelle")) ? head.get("libelle") : "Reprise " + head.get("piece"), "IMPORT", head.get("piece"),
                        entryLines, false));
                lines += entryLines.size();
                created.add(entry.provisionalNumber());
                if (validate) {
                    entries.validate(entry.provisionalNumber());
                    validated++;
                }
            } catch (RuntimeException exception) {
                errors.add("Piece " + group.getKey().replace('|', ' ') + " : " + exception.getMessage());
            }
        }
        return new ImportResult(created.size(), lines, validated, created, errors);
    }

    // ================================================================== soldes d'ouverture

    /**
     * Soldes d'ouverture : colonnes {@code compte;tiers;reference;echeance;debit;credit;libelle}. Une
     * seule piece au premier jour de l'exercice, dans le journal des a-nouveaux ; une ligne par piece
     * ouverte pour un compte lettrable, pour que le lettrage se poursuive.
     */
    public ImportResult importOpening(String yearCode, byte[] file) {
        statements.require(RUN);
        AccFiscalYear year = statements.year(yearCode);
        List<AccountingRequests.EntryLine> lines = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int number = 0;
        for (Map<String, String> row : rows(file)) {
            number++;
            try {
                lines.add(line(row));
            } catch (RuntimeException exception) {
                errors.add("Ligne " + number + " : " + exception.getMessage());
            }
        }
        if (!errors.isEmpty()) {
            return new ImportResult(0, 0, 0, List.of(), errors);
        }
        if (lines.isEmpty()) {
            throw new BadRequestException("Le fichier ne contient aucun solde");
        }
        AccEntry entry = entries.post(new AccountingEntryService.AutoPost(settings.text("accounting.closing.opening-journal", "AN"), year.getStartDate(), null,
                "Reprise des soldes d'ouverture " + year.getCode(), EntryOrigin.IMPORT, "OPENING_IMPORT", year.getCode(), lines, actors.resolve("SYSTEM"),
                true, null));
        boolean ok = entry.getStatus() == EntryStatus.VALIDATED;
        return new ImportResult(1, lines.size(), ok ? 1 : 0, List.of(ok ? entry.getEntryNumber() : entry.getProvisionalNumber()),
                ok ? List.of() : List.of("Piece en attente : " + entry.getPendingDetail()));
    }

    // ================================================================== double tenue

    public record Gap(String account, BigDecimal ours, BigDecimal theirs, BigDecimal difference) {
    }

    public record Comparison(LocalDate date, String format, BigDecimal oursTotal, BigDecimal theirsTotal, List<Gap> gaps, int accounts, boolean identical) {
    }

    /**
     * Compare la balance a une date avec celle d'un autre logiciel ({@code compte;debit;credit} ou
     * {@code compte;solde}), comptes passes par la correspondance du format (et completes a sa longueur).
     */
    @Transactional(readOnly = true)
    public Comparison compare(LocalDate at, String format, byte[] file) {
        statements.require(RUN, AccountingExportService.RUN);
        LocalDate date = at == null ? LocalDate.now() : at;
        String target = AccountingExportService.format(format == null ? "CSV" : format);
        Map<String, String> mapped = new LinkedHashMap<>();
        for (AccExportMapping mapping : mappings.findAllByFormatOrderByKindAscSourceAsc(target)) {
            if ("ACCOUNT".equals(mapping.getKind())) {
                mapped.put(mapping.getSource(), mapping.getTarget());
            }
        }
        int length = switch (target) {
            case "SAGE100" -> integer("accounting.export.sage100.account-length", 8);
            case "SAGEX3" -> integer("accounting.export.sagex3.account-length", 0);
            case "ODOO" -> integer("accounting.export.odoo.account-length", 0);
            default -> 0;
        };
        Map<String, BigDecimal> ours = new TreeMap<>();
        LocalDate yearStart = statements.yearOf(date).getStartDate();
        statements.balances(statements.scopeStart(date), date).forEach((account, amount) -> {
            if (FinancialStatementService.isBalanceSheet(account)) {
                ours.merge(AccountingExportService.account(account, mapped, length), amount, BigDecimal::add);
            }
        });
        statements.balances(yearStart, date).forEach((account, amount) -> {
            if (FinancialStatementService.isIncome(account)) {
                ours.merge(AccountingExportService.account(account, mapped, length), amount, BigDecimal::add);
            }
        });
        // Resultat des exercices anterieurs non clos : en instance d'affectation (130), comme dans l'export en balance.
        LocalDate scope = statements.scopeStart(date);
        if (scope.isBefore(yearStart)) {
            BigDecimal earlier = statements.incomeResult(scope, yearStart.minusDays(1));
            if (earlier.signum() != 0) {
                ours.merge(AccountingExportService.account(FinancialStatementService.PENDING_RESULT_ACCOUNT, mapped, length), earlier.negate(), BigDecimal::add);
            }
        }
        Map<String, BigDecimal> theirs = new TreeMap<>();
        for (Map<String, String> row : rows(file)) {
            String account = required(row, "compte");
            BigDecimal amount = row.containsKey("solde") && StringUtils.hasText(row.get("solde")) ? amount(row.get("solde"))
                    : amount(row.get("debit")).subtract(amount(row.get("credit")));
            theirs.merge(account, amount, BigDecimal::add);
        }
        java.util.Set<String> accounts = new java.util.TreeSet<>(ours.keySet());
        accounts.addAll(theirs.keySet());
        List<Gap> gaps = new ArrayList<>();
        for (String account : accounts) {
            BigDecimal mine = ours.getOrDefault(account, BigDecimal.ZERO);
            BigDecimal other = theirs.getOrDefault(account, BigDecimal.ZERO);
            if (mine.compareTo(other) != 0) {
                gaps.add(new Gap(account, mine, other, mine.subtract(other)));
            }
        }
        return new Comparison(date, target, ours.values().stream().map(BigDecimal::abs).reduce(BigDecimal.ZERO, BigDecimal::add),
                theirs.values().stream().map(BigDecimal::abs).reduce(BigDecimal.ZERO, BigDecimal::add), gaps, accounts.size(), gaps.isEmpty());
    }

    // ================================================================== lecture des fichiers

    /** Lignes du fichier, par nom de colonne en minuscules sans accents. */
    static List<Map<String, String>> rows(byte[] file) {
        if (file == null || file.length == 0) {
            throw new BadRequestException("Fichier vide");
        }
        String text = decode(file);
        if (text.startsWith("﻿")) {
            text = text.substring(1);
        }
        List<String> lines = text.lines().filter(StringUtils::hasText).toList();
        if (lines.size() < 2) {
            throw new BadRequestException("Le fichier doit avoir une ligne d'en-tete et au moins une ligne");
        }
        char separator = lines.get(0).contains(";") ? ';' : ',';
        List<String> header = split(lines.get(0), separator).stream().map(AccountingImportService::key).toList();
        List<Map<String, String>> rows = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            List<String> cells = split(line, separator);
            Map<String, String> row = new LinkedHashMap<>();
            for (int index = 0; index < header.size(); index++) {
                row.put(header.get(index), index < cells.size() ? cells.get(index).trim() : "");
            }
            rows.add(row);
        }
        return rows;
    }

    private static String decode(byte[] file) {
        try {
            return StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(file)).toString();
        } catch (java.nio.charset.CharacterCodingException exception) {
            return new String(file, Charset.forName("windows-1252"));
        }
    }

    /** Decoupe une ligne CSV, guillemets compris. */
    static List<String> split(String line, char separator) {
        List<String> cells = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    cell.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == separator && !quoted) {
                cells.add(cell.toString());
                cell.setLength(0);
            } else {
                cell.append(character);
            }
        }
        cells.add(cell.toString());
        return cells;
    }

    private static String key(String value) {
        String normalized = java.text.Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return switch (normalized) {
            case "account", "compte general", "numero de compte" -> "compte";
            case "third party", "compte tiers" -> "tiers";
            case "label", "libelle ecriture" -> "libelle";
            case "due date", "date d'echeance" -> "echeance";
            case "numero de piece", "piece comptable", "entry" -> "piece";
            case "balance" -> "solde";
            default -> normalized;
        };
    }

    private static AccountingRequests.EntryLine line(Map<String, String> row) {
        BigDecimal debit = amount(row.get("debit"));
        BigDecimal credit = amount(row.get("credit"));
        if (debit.signum() < 0 || credit.signum() < 0 || (debit.signum() == 0) == (credit.signum() == 0)) {
            throw new BadRequestException("Chaque ligne porte un debit ou un credit positif (compte " + row.get("compte") + ")");
        }
        return new AccountingRequests.EntryLine(required(row, "compte"), blank(row.get("tiers")), blank(row.get("libelle")),
                debit.signum() > 0 ? debit : null, credit.signum() > 0 ? credit : null, null, null, null, null, null,
                StringUtils.hasText(row.get("echeance")) ? date(row.get("echeance")) : null, blank(row.get("reference")));
    }

    private static String required(Map<String, String> row, String key) {
        String value = row.get(key);
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException("Colonne " + key + " manquante ou vide");
        }
        return value.trim();
    }

    private static String blank(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    static LocalDate date(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException("Date manquante");
        }
        String text = value.trim();
        for (DateTimeFormatter format : List.of(DateTimeFormatter.ISO_LOCAL_DATE, DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("ddMMyy"), DateTimeFormatter.BASIC_ISO_DATE)) {
            try {
                return LocalDate.parse(text, format);
            } catch (DateTimeParseException ignored) {
                // format suivant
            }
        }
        throw new BadRequestException("Date illisible : " + text);
    }

    static BigDecimal amount(String value) {
        if (!StringUtils.hasText(value)) {
            return BigDecimal.ZERO;
        }
        String text = value.trim().replace(" ", "").replace(" ", "").replace(" ", "");
        if (text.contains(",") && text.contains(".")) {
            text = text.replace(".", "").replace(',', '.');
        } else {
            text = text.replace(',', '.');
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException exception) {
            throw new BadRequestException("Montant illisible : " + value);
        }
    }

    private int integer(String key, int fallback) {
        try {
            return Integer.parseInt(settings.text(key, String.valueOf(fallback)).trim());
        } catch (RuntimeException exception) {
            return fallback;
        }
    }
}
