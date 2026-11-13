package com.sni.bokaticowork.features.accounting.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.accounting.model.AccArchive;
import com.sni.bokaticowork.features.accounting.model.AccFiscalYear;
import com.sni.bokaticowork.features.accounting.model.AccGeneratedReport;
import com.sni.bokaticowork.features.accounting.repository.AccArchiveRepository;
import com.sni.bokaticowork.features.accounting.repository.AccGeneratedReportRepository;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Archive annuelle de la comptabilite (lot F14, plan des etats, section 10) : un ZIP avec le
 * fichier des ecritures de l'exercice, la balance, le bilan, le compte de resultat et le tableau des
 * flux, la liasse, les editions definitives (donnees et PDF figes), les lettrages et la liste des
 * pieces justificatives. Un sommaire ({@code manifest.json}) donne l'empreinte SHA-256 de chaque
 * fichier ; l'archive elle-meme est tracee avec son empreinte, pour la conservation de 10 ans.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AccountingArchiveService {

    public static final String RUN = "ACCOUNTING_ARCHIVE:RUN";

    private final JdbcTemplate jdbc;
    private final FinancialStatementService statements;
    private final ClosingService closing;
    private final AccountingExportService exports;
    private final AccArchiveRepository archives;
    private final AccGeneratedReportRepository reports;
    private final CashActorResolver actors;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<LiasseService> liasse;
    private final ObjectProvider<FixedAssetReportService> exporter;

    public record Archive(AccArchive record, byte[] content) {
    }

    public Archive archive(String yearCode) {
        statements.require(RUN);
        AccFiscalYear year = statements.year(yearCode);
        Map<String, byte[]> files = new LinkedHashMap<>();
        files.put("ecritures.csv", exports.csv(exports.lines(year.getStartDate(), year.getEndDate(), null), Map.of(), Map.of()));
        files.put("balance.csv", balance(year));
        FixedAssetReportService renderer = exporter.getIfAvailable();
        for (String code : List.of(FinancialStatementService.BALANCE_SHEET, FinancialStatementService.INCOME_STATEMENT, FinancialStatementService.CASH_FLOW)) {
            FinancialStatementService.Statement statement = closing.statement(code, year);
            files.put("etats/" + code.toLowerCase() + ".json", json(statement));
            if (renderer != null) {
                files.put("etats/" + code.toLowerCase() + ".csv", renderer.csv(statements.table(statement)));
            }
        }
        LiasseService builder = liasse.getIfAvailable();
        if (builder != null) {
            files.put("liasse/liasse.xls", builder.excel(builder.build(year.getCode())));
        }
        files.put("notes.json", json(closing.notes(year.getCode())));
        for (AccGeneratedReport report : reports.findAllByFiscalYearCodeOrderByGeneratedAtDesc(year.getCode())) {
            files.put("editions/" + report.getNumber() + ".json", report.getDataJson().getBytes(StandardCharsets.UTF_8));
            if (report.getPdf() != null) {
                files.put("editions/" + report.getNumber() + ".pdf", report.getPdf());
            }
        }
        files.put("lettrages.csv", letterings(year));
        files.put("pieces-justificatives.csv", attachments(year));
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("exercice", year.getCode());
        manifest.put("du", year.getStartDate().toString());
        manifest.put("au", year.getEndDate().toString());
        manifest.put("statut", year.getStatus().name());
        manifest.put("generePar", actors.resolve("SYSTEM"));
        manifest.put("genereLe", Instant.now().toString());
        List<Map<String, Object>> list = new ArrayList<>();
        files.forEach((name, content) -> list.add(Map.of("fichier", name, "octets", content.length, "sha256", AccountingExportService.sha256(content))));
        manifest.put("fichiers", list);
        byte[] manifestJson = json(manifest);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(buffer, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, byte[]> file : files.entrySet()) {
                zip.putNextEntry(new ZipEntry(file.getKey()));
                zip.write(file.getValue());
                zip.closeEntry();
            }
            zip.putNextEntry(new ZipEntry("manifest.json"));
            zip.write(manifestJson);
            zip.closeEntry();
        } catch (IOException exception) {
            throw new BadRequestException("Archive impossible : " + exception.getMessage());
        }
        byte[] content = buffer.toByteArray();
        AccArchive record = archives.save(AccArchive.builder()
                .number("ARC-" + year.getCode() + "-" + (archives.findAllByFiscalYearCodeOrderByCreatedAtDesc(year.getCode()).size() + 1))
                .fiscalYearCode(year.getCode()).fileName("archive-comptable-" + year.getCode() + ".zip").sizeBytes((long) content.length)
                .contentHash(AccountingExportService.sha256(content)).manifest(new String(manifestJson, StandardCharsets.UTF_8))
                .createdBy(actors.resolve("SYSTEM")).createdAt(Instant.now()).build());
        return new Archive(record, content);
    }

    @Transactional(readOnly = true)
    public List<AccArchive> archives(String yearCode) {
        statements.require(RUN, FinancialStatementService.READ);
        return archives.findAllByFiscalYearCodeOrderByCreatedAtDesc(statements.year(yearCode).getCode());
    }

    // ================================================================== fichiers

    private byte[] balance(AccFiscalYear year) {
        StringBuilder out = new StringBuilder("﻿Compte;Debit;Credit;Solde\r\n");
        Map<String, BigDecimal[]> totals = new java.util.TreeMap<>();
        jdbc.query("SELECT l.account_number, COALESCE(SUM(l.debit), 0), COALESCE(SUM(l.credit), 0) FROM acc_entry_line l JOIN acc_entry e ON e.id = l.entry_id "
                        + "WHERE e.status IN ('VALIDATED', 'REVERSED') AND e.entry_date BETWEEN ? AND ? GROUP BY l.account_number",
                rs -> {
                    totals.put(rs.getString(1), new BigDecimal[]{rs.getBigDecimal(2), rs.getBigDecimal(3)});
                }, year.getStartDate(), year.getEndDate());
        totals.forEach((account, values) -> out.append(account).append(';').append(AccountingExportService.comma(values[0])).append(';')
                .append(AccountingExportService.comma(values[1])).append(';').append(AccountingExportService.comma(values[0].subtract(values[1]))).append("\r\n"));
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] letterings(AccFiscalYear year) {
        StringBuilder out = new StringBuilder("﻿Code;Compte;Tiers;Type;Etat;Debit;Credit;Ecart;Lettre par;Lettre le;Delettre le;Motif\r\n");
        jdbc.query("SELECT DISTINCT lt.code, lt.account_number, lt.third_party_code, lt.kind, lt.status, lt.total_debit, lt.total_credit, lt.difference, "
                        + "lt.lettered_by, lt.lettered_at, lt.unlettered_at, lt.unlettering_reason FROM acc_lettering lt "
                        + "JOIN acc_lettering_line ll ON ll.lettering_id = lt.id JOIN acc_entry_line l ON l.id = ll.entry_line_id "
                        + "JOIN acc_entry e ON e.id = l.entry_id WHERE e.entry_date BETWEEN ? AND ? ORDER BY lt.account_number, lt.code",
                rs -> {
                    out.append(rs.getString(1)).append(';').append(rs.getString(2)).append(';').append(nz(rs.getString(3))).append(';')
                            .append(rs.getString(4)).append(';').append(rs.getString(5)).append(';').append(AccountingExportService.comma(rs.getBigDecimal(6)))
                            .append(';').append(AccountingExportService.comma(rs.getBigDecimal(7))).append(';').append(AccountingExportService.comma(rs.getBigDecimal(8)))
                            .append(';').append(nz(rs.getString(9))).append(';').append(rs.getTimestamp(10)).append(';')
                            .append(rs.getTimestamp(11) == null ? "" : rs.getTimestamp(11)).append(';').append(nz(rs.getString(12)).replace(';', ',')).append("\r\n");
                }, year.getStartDate(), year.getEndDate());
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] attachments(AccFiscalYear year) {
        StringBuilder out = new StringBuilder("﻿Piece;Journal;Date;Libelle;Origine;Source;Pieces jointes\r\n");
        jdbc.query("SELECT COALESCE(entry_number, provisional_number), journal_code, entry_date, label, origin, COALESCE(source_type, '') || ' ' || COALESCE(source_code, ''), "
                        + "COALESCE(CAST(attachments_json AS TEXT), '') FROM acc_entry WHERE status IN ('VALIDATED', 'REVERSED') AND entry_date BETWEEN ? AND ? ORDER BY entry_date, id",
                rs -> {
                    out.append(rs.getString(1)).append(';').append(rs.getString(2)).append(';').append(rs.getDate(3)).append(';')
                            .append(nz(rs.getString(4)).replace(';', ',')).append(';').append(rs.getString(5)).append(';').append(rs.getString(6).trim()).append(';')
                            .append(nz(rs.getString(7)).replace(';', ',').replace('\n', ' ')).append("\r\n");
                }, year.getStartDate(), year.getEndDate());
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] json(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(value);
        } catch (Exception exception) {
            throw new BadRequestException("Archive impossible : " + exception.getMessage());
        }
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }
}
