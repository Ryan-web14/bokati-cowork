package com.sni.bokaticowork.features.accounting.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.accounting.model.AccExportBatch;
import com.sni.bokaticowork.features.accounting.model.AccExportMapping;
import com.sni.bokaticowork.features.accounting.repository.AccExportBatchRepository;
import com.sni.bokaticowork.features.accounting.repository.AccExportMappingRepository;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Exports pour l'expert-comptable (lot F14, plan, section 13 ; decision 9) : fichier des ecritures
 * ou balance, aux formats d'import de Sage 100, Sage X3 et Odoo, plus un CSV generique. Seules les
 * pieces validees partent ; une piece acceptee desequilibree est exclue des formats cibles (ils la
 * refuseraient) et marquee dans le CSV. Comptes et journaux passent par une table de
 * correspondance par format ; a defaut, le compte est complete a la longueur du format.
 *
 * <p>Mode balance : une seule piece, au dernier jour, avec le solde de chaque compte (par tiers
 * pour les comptes de tiers) ; reimportee dans le logiciel cible, elle redonne la meme balance.</p>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AccountingExportService {

    public static final String RUN = "ACCOUNTING_EXPORT:RUN";
    public static final String MAPPING = "ACCOUNTING_EXPORT:MAPPING";
    public static final List<String> FORMATS = List.of("SAGE100", "SAGEX3", "ODOO", "CSV");

    private final JdbcTemplate jdbc;
    private final FinancialStatementService statements;
    private final AccExportMappingRepository mappings;
    private final AccExportBatchRepository batches;
    private final AccountingSettings settings;
    private final CashActorResolver actors;

    /** Une ligne exportee. */
    public record Line(String entryNumber, String journal, LocalDate date, LocalDate documentDate, String entryLabel, String account,
                       String thirdParty, String label, BigDecimal debit, BigDecimal credit, String reference, LocalDate dueDate,
                       String currency, BigDecimal foreignAmount, boolean unbalanced) {
    }

    /** Fichier produit et son lot. */
    public record ExportFile(String fileName, String contentType, byte[] content, AccExportBatch batch) {
    }

    public record ExportRequest(String format, String mode, LocalDate from, LocalDate to, String journal) {
    }

    // ================================================================== export

    public ExportFile export(ExportRequest request) {
        statements.require(RUN);
        String format = format(request == null ? null : request.format());
        String mode = request.mode() == null ? "ENTRIES" : request.mode().trim().toUpperCase(Locale.ROOT);
        if (!List.of("ENTRIES", "BALANCE").contains(mode)) {
            throw new BadRequestException("Mode ENTRIES (ecritures) ou BALANCE");
        }
        LocalDate to = request.to() == null ? LocalDate.now() : request.to();
        LocalDate from = request.from() == null ? statements.yearOf(to).getStartDate() : request.from();
        if (from.isAfter(to)) {
            throw new BadRequestException("La periode commence apres sa fin");
        }
        List<Line> lines = "BALANCE".equals(mode) ? balanceLines(to) : lines(from, to, request.journal());
        int excluded = 0;
        if (!"CSV".equals(format)) {
            List<Line> kept = new ArrayList<>();
            java.util.Set<String> skipped = new java.util.HashSet<>();
            for (Line line : lines) {
                if (line.unbalanced()) {
                    skipped.add(line.entryNumber());
                } else {
                    kept.add(line);
                }
            }
            excluded = skipped.size();
            lines = kept;
        }
        Map<String, String> accountMap = new LinkedHashMap<>();
        Map<String, String> journalMap = new LinkedHashMap<>();
        for (AccExportMapping mapping : mappings.findAllByFormatOrderByKindAscSourceAsc(format)) {
            ("ACCOUNT".equals(mapping.getKind()) ? accountMap : journalMap).put(mapping.getSource(), mapping.getTarget());
        }
        byte[] content = switch (format) {
            case "SAGE100" -> sage100(lines, accountMap, journalMap);
            case "SAGEX3" -> sageX3(lines, accountMap, journalMap);
            case "ODOO" -> odoo(lines, accountMap, journalMap);
            default -> csv(lines, accountMap, journalMap);
        };
        BigDecimal debit = lines.stream().map(Line::debit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credit = lines.stream().map(Line::credit).reduce(BigDecimal.ZERO, BigDecimal::add);
        String extension = "ODOO".equals(format) || "CSV".equals(format) ? "csv" : "txt";
        String fileName = "export-" + format.toLowerCase(Locale.ROOT) + "-" + mode.toLowerCase(Locale.ROOT) + "-" + from + "-" + to + "." + extension;
        AccExportBatch batch = batches.save(AccExportBatch.builder()
                .number("EXP-" + format + "-" + String.format("%05d", batches.countByFormat(format) + 1))
                .format(format).mode(mode).periodFrom(from).periodTo(to).journalCode(StringUtils.hasText(request.journal()) ? request.journal().trim() : null)
                .entryCount((int) lines.stream().map(Line::entryNumber).distinct().count()).lineCount(lines.size()).totalDebit(debit).totalCredit(credit)
                .excluded(excluded).fileName(fileName).contentHash(sha256(content)).createdBy(actors.resolve("SYSTEM")).createdAt(Instant.now()).build());
        String type = "SAGE100".equals(format) ? "text/plain; charset=windows-1252" : "text/plain; charset=utf-8";
        return new ExportFile(fileName, type, content, batch);
    }

    @Transactional(readOnly = true)
    public List<AccExportBatch> batches() {
        statements.require(RUN);
        return batches.findTop100ByOrderByCreatedAtDesc();
    }

    // ================================================================== lignes

    /** Lignes des pieces validees de la periode (hors classe 9), dans l'ordre des pieces. */
    List<Line> lines(LocalDate from, LocalDate to, String journal) {
        List<Line> lines = new ArrayList<>();
        jdbc.query("SELECT e.entry_number, e.journal_code, e.entry_date, e.document_date, e.label, l.account_number, l.third_party_code, l.label, "
                        + "l.debit, l.credit, l.reference, l.due_date, l.currency, l.foreign_amount, e.accepted_unbalanced "
                        + "FROM acc_entry e JOIN acc_entry_line l ON l.entry_id = e.id WHERE e.status IN ('VALIDATED', 'REVERSED') "
                        + "AND e.entry_date BETWEEN ? AND ? AND l.account_number NOT LIKE '9%' AND (CAST(? AS VARCHAR) IS NULL OR e.journal_code = ?) "
                        + "ORDER BY e.entry_date, e.journal_code, e.entry_number, l.line_no",
                rs -> {
                    lines.add(new Line(rs.getString(1), rs.getString(2), rs.getDate(3).toLocalDate(),
                            rs.getDate(4) == null ? null : rs.getDate(4).toLocalDate(), rs.getString(5), rs.getString(6), rs.getString(7),
                            rs.getString(8), rs.getBigDecimal(9), rs.getBigDecimal(10), rs.getString(11),
                            rs.getDate(12) == null ? null : rs.getDate(12).toLocalDate(), rs.getString(13), rs.getBigDecimal(14), rs.getBoolean(15)));
                }, from, to, blank(journal), blank(journal));
        return lines;
    }

    /** Balance a une date, en une piece : solde de chaque compte, par tiers pour les comptes de tiers. */
    List<Line> balanceLines(LocalDate date) {
        LocalDate scope = statements.scopeStart(date);
        LocalDate yearStart = statements.yearOf(date).getStartDate();
        Map<String, BigDecimal> balances = new TreeMap<>();
        jdbc.query("SELECT l.account_number, COALESCE(l.third_party_code, ''), COALESCE(SUM(l.debit), 0) - COALESCE(SUM(l.credit), 0) "
                        + "FROM acc_entry_line l JOIN acc_entry e ON e.id = l.entry_id WHERE e.status IN ('VALIDATED', 'REVERSED') AND e.accepted_unbalanced = FALSE "
                        + "AND l.account_number NOT LIKE '9%' AND e.entry_date BETWEEN ? AND ? "
                        + "AND (l.account_number SIMILAR TO '[12345]%' OR e.entry_date >= ?) GROUP BY 1, 2",
                rs -> {
                    balances.merge(rs.getString(1) + "|" + rs.getString(2), rs.getBigDecimal(3), BigDecimal::add);
                }, scope, date, yearStart);
        // Resultat des exercices anterieurs non clos : en instance d'affectation, pour que la piece s'equilibre.
        if (scope.isBefore(yearStart)) {
            BigDecimal earlier = statements.incomeResult(scope, yearStart.minusDays(1));
            if (earlier.signum() != 0) {
                // Les comptes de gestion de ces exercices sont deja exclus ; leur resultat passe en 130.
                balances.merge(FinancialStatementService.PENDING_RESULT_ACCOUNT + "|", earlier.negate(), BigDecimal::add);
            }
        }
        String number = "BAL-" + date.format(DateTimeFormatter.BASIC_ISO_DATE);
        String journal = settings.text("accounting.closing.opening-journal", "AN");
        List<Line> lines = new ArrayList<>();
        balances.forEach((key, amount) -> {
            if (amount.signum() == 0) {
                return;
            }
            String[] parts = key.split("\\|", -1);
            lines.add(new Line(number, journal, date, date, "Balance au " + date, parts[0], parts[1].isEmpty() ? null : parts[1], "Solde au " + date,
                    amount.signum() > 0 ? amount : BigDecimal.ZERO, amount.signum() < 0 ? amount.negate() : BigDecimal.ZERO, null, null, null, null, false));
        });
        return lines;
    }

    // ================================================================== formats

    /**
     * Sage 100 Comptabilite, import parametrable : une ligne par mouvement, separateur point-virgule,
     * date JJMMAA, montants a virgule, encodage Windows-1252, sans en-tete. Colonnes : journal ; date ;
     * piece ; compte general ; compte tiers ; libelle ; debit ; credit ; echeance ; reference.
     */
    byte[] sage100(List<Line> lines, Map<String, String> accounts, Map<String, String> journals) {
        DateTimeFormatter date = DateTimeFormatter.ofPattern("ddMMyy");
        int length = integer("accounting.export.sage100.account-length", 8);
        StringBuilder out = new StringBuilder();
        for (Line line : lines) {
            out.append(cut(journal(line.journal(), journals), 6)).append(';')
                    .append(line.date().format(date)).append(';')
                    .append(cut(line.entryNumber(), 13)).append(';')
                    .append(account(line.account(), accounts, length)).append(';')
                    .append(cut(nz(line.thirdParty()), 17)).append(';')
                    .append(cut(clean(label(line)), 35)).append(';')
                    .append(comma(line.debit())).append(';')
                    .append(comma(line.credit())).append(';')
                    .append(line.dueDate() == null ? "" : line.dueDate().format(date)).append(';')
                    .append(cut(clean(nz(line.reference())), 17)).append("\r\n");
        }
        return out.toString().getBytes(Charset.forName("windows-1252"));
    }

    /**
     * Sage X3, import de pieces a deux types d'enregistrement (modele d'import a declarer dans X3) :
     * {@code G} en-tete de piece (type, site, journal, date AAAAMMJJ, reference, libelle, devise) et
     * {@code D} ligne (numero, compte, tiers, libelle, sens 1 debit ou -1 credit, montant, echeance).
     */
    byte[] sageX3(List<Line> lines, Map<String, String> accounts, Map<String, String> journals) {
        DateTimeFormatter date = DateTimeFormatter.BASIC_ISO_DATE;
        String type = settings.text("accounting.export.sagex3.entry-type", "OD");
        String site = settings.text("accounting.export.sagex3.site", "");
        int length = integer("accounting.export.sagex3.account-length", 0);
        StringBuilder out = new StringBuilder();
        String current = null;
        int number = 0;
        for (Line line : lines) {
            if (!line.entryNumber().equals(current)) {
                current = line.entryNumber();
                number = 0;
                out.append("G;").append(type).append(';').append(site).append(';').append(journal(line.journal(), journals)).append(';')
                        .append(line.date().format(date)).append(';').append(line.entryNumber()).append(';').append(clean(nz(line.entryLabel())))
                        .append(";XAF\r\n");
            }
            number++;
            boolean debit = line.debit().signum() > 0;
            out.append("D;").append(number).append(';').append(account(line.account(), accounts, length)).append(';').append(nz(line.thirdParty())).append(';')
                    .append(clean(label(line))).append(';').append(debit ? "1" : "-1").append(';').append(plain(debit ? line.debit() : line.credit())).append(';')
                    .append(line.dueDate() == null ? "" : line.dueDate().format(date)).append("\r\n");
        }
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Odoo, import des pieces comptables ({@code account.move}) : la premiere ligne d'une piece porte
     * son identifiant externe, son journal, sa date et sa reference ; les suivantes ne portent que
     * les colonnes {@code line_ids/...}. Separateur virgule, point decimal, UTF-8.
     */
    byte[] odoo(List<Line> lines, Map<String, String> accounts, Map<String, String> journals) {
        int length = integer("accounting.export.odoo.account-length", 0);
        StringBuilder out = new StringBuilder("id,journal_id,date,ref,line_ids/account_id,line_ids/partner_id,line_ids/name,line_ids/debit,line_ids/credit\r\n");
        String current = null;
        for (Line line : lines) {
            boolean first = !line.entryNumber().equals(current);
            current = line.entryNumber();
            out.append(first ? quote("bokati_" + line.entryNumber().replaceAll("[^A-Za-z0-9_]", "_")) : "").append(',')
                    .append(first ? quote(journal(line.journal(), journals)) : "").append(',')
                    .append(first ? line.date().toString() : "").append(',')
                    .append(first ? quote(nz(line.entryLabel())) : "").append(',')
                    .append(quote(account(line.account(), accounts, length))).append(',')
                    .append(quote(nz(line.thirdParty()))).append(',')
                    .append(quote(label(line))).append(',')
                    .append(plain(line.debit())).append(',')
                    .append(plain(line.credit())).append("\r\n");
        }
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** CSV generique, complet : toutes les colonnes, pieces acceptees desequilibrees marquees. */
    byte[] csv(List<Line> lines, Map<String, String> accounts, Map<String, String> journals) {
        StringBuilder out = new StringBuilder("﻿Journal;Date;Piece;Date piece;Compte;Tiers;Libelle;Debit;Credit;Reference;Echeance;Devise;Montant devise;Desequilibree\r\n");
        for (Line line : lines) {
            out.append(journal(line.journal(), journals)).append(';').append(line.date()).append(';').append(line.entryNumber()).append(';')
                    .append(line.documentDate() == null ? "" : line.documentDate()).append(';').append(account(line.account(), accounts, 0)).append(';')
                    .append(nz(line.thirdParty())).append(';').append(clean(label(line))).append(';').append(comma(line.debit())).append(';')
                    .append(comma(line.credit())).append(';').append(clean(nz(line.reference()))).append(';')
                    .append(line.dueDate() == null ? "" : line.dueDate()).append(';').append(nz(line.currency())).append(';')
                    .append(line.foreignAmount() == null ? "" : comma(line.foreignAmount())).append(';').append(line.unbalanced() ? "OUI" : "").append("\r\n");
        }
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ================================================================== correspondance

    public record MappingRequest(String format, String kind, String source, String target) {
    }

    @Transactional(readOnly = true)
    public List<AccExportMapping> mappings(String format) {
        statements.require(RUN, MAPPING);
        return mappings.findAllByFormatOrderByKindAscSourceAsc(format(format));
    }

    public AccExportMapping saveMapping(MappingRequest request) {
        statements.require(MAPPING);
        String format = format(request == null ? null : request.format());
        String kind = request.kind() == null ? "" : request.kind().trim().toUpperCase(Locale.ROOT);
        if (!List.of("ACCOUNT", "JOURNAL").contains(kind) || !StringUtils.hasText(request.source()) || !StringUtils.hasText(request.target())) {
            throw new BadRequestException("Indiquez le type (ACCOUNT ou JOURNAL), la valeur source et la valeur cible");
        }
        AccExportMapping mapping = mappings.findByFormatAndKindAndSource(format, kind, request.source().trim())
                .orElseGet(() -> AccExportMapping.builder().format(format).kind(kind).source(request.source().trim()).build());
        mapping.setTarget(request.target().trim());
        mapping.setCreatedBy(actors.resolve("SYSTEM"));
        return mappings.save(mapping);
    }

    public void deleteMapping(Long id) {
        statements.require(MAPPING);
        if (!mappings.existsById(id)) {
            throw new ResourceNotFoundException("Correspondance introuvable : " + id);
        }
        mappings.deleteById(id);
    }

    /** Compte cible : correspondance, sinon prefixe le plus long en correspondance, sinon complete a la longueur. */
    static String account(String number, Map<String, String> mapped, int length) {
        if (mapped.containsKey(number)) {
            return mapped.get(number);
        }
        String result = number;
        if (length > 0 && result.length() < length) {
            result = result + "0".repeat(length - result.length());
        }
        return result;
    }

    private static String journal(String code, Map<String, String> mapped) {
        return mapped.getOrDefault(code, code);
    }

    // ================================================================== outils

    static String format(String value) {
        String format = value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replace(" ", "").replace("_", "");
        if (!FORMATS.contains(format)) {
            throw new BadRequestException("Format SAGE100, SAGEX3, ODOO ou CSV");
        }
        return format;
    }

    private static String label(Line line) {
        return StringUtils.hasText(line.label()) ? line.label() : nz(line.entryLabel());
    }

    private static String clean(String value) {
        return value == null ? "" : value.replace(';', ',').replace('\r', ' ').replace('\n', ' ').trim();
    }

    private static String quote(String value) {
        return "\"" + (value == null ? "" : value.replace("\"", "\"\"").replace('\r', ' ').replace('\n', ' ')) + "\"";
    }

    private static String cut(String value, int max) {
        return value == null ? "" : value.length() > max ? value.substring(0, max) : value;
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }

    private static String blank(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    static String plain(BigDecimal value) {
        return value == null || value.signum() == 0 ? "0" : value.setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    static String comma(BigDecimal value) {
        return plain(value).replace('.', ',');
    }

    private int integer(String key, int fallback) {
        try {
            return Integer.parseInt(settings.text(key, String.valueOf(fallback)).trim());
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
