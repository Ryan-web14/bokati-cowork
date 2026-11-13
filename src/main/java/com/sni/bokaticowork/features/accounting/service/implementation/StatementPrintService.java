package com.sni.bokaticowork.features.accounting.service.implementation;

import com.sni.bokaticowork.features.fund.voucher.VoucherService;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Releves imprimables des fournisseurs et des partenaires (modeles de documents, lot D4). Un releve
 * n'est pas une piece comptable : il ne prend pas de numero au registre (pas de QR de verification)
 * et porte la reference {@code RLF-<fournisseur>-<date>} ou {@code RLP-<partenaire>-<date>}.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StatementPrintService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PurchaseService purchases;
    private final PartnerAccountService partners;
    private final VoucherService vouchers;
    private final CashActorResolver actors;

    public byte[] supplierStatement(String code, LocalDate from, LocalDate to) {
        PurchaseService.Statement statement = purchases.statement(code, from, to);
        List<VoucherService.Section> sections = new ArrayList<>();
        sections.add(new VoucherService.Section("Fournisseur").pair("Code", statement.supplier().getCode()).pair("Nom", statement.supplier().getName())
                .pair("NIU", statement.supplier().getNiu()).pair("RCCM", statement.supplier().getRccm()).pair("Compte", statement.supplier().getAccountNumber())
                .pair("Période", period(statement.from(), statement.to())));
        VoucherService.Section lines = new VoucherService.Section("Écritures").columns(
                List.of("Date", "Écriture", "Journal", "Libellé", "Référence", "Débit", "Crédit", "Solde"),
                List.of(false, false, false, false, false, true, true, true));
        statement.lines().forEach(line -> lines.row(DAY.format(line.date()), line.entryNumber(), line.journal(), line.label(), line.reference(),
                VoucherService.money(line.debit()), VoucherService.money(line.credit()), VoucherService.money(line.balance())));
        sections.add(lines);
        sections.add(balances(statement.opening(), statement.closing(), "Solde dû au fournisseur"));
        return vouchers.render("Relevé fournisseur", issued("RLF", statement.supplier().getCode(), statement.closing()), sections, null, null,
                List.of(new VoucherService.Signature("Comptable", null, null), new VoucherService.Signature("Fournisseur, pour accord", null, null)));
    }

    public byte[] partnerStatement(String code, LocalDate from, LocalDate to) {
        PartnerAccountService.Statement statement = partners.statement(code, from, to);
        List<VoucherService.Section> sections = new ArrayList<>();
        sections.add(new VoucherService.Section("Partenaire").pair("Code", statement.partner().getCode()).pair("Nom", statement.partner().getPartnerName())
                .pair("Compte", statement.partner().getAccountNumber()).pair("Période", period(statement.from(), statement.to())));
        VoucherService.Section lines = new VoucherService.Section("Écritures").columns(
                List.of("Date", "Écriture", "Libellé", "Référence", "Débit", "Crédit", "Solde"),
                List.of(false, false, false, false, true, true, true));
        statement.lines().forEach(line -> lines.row(DAY.format(line.date()), line.entryNumber(), line.label(), line.reference(),
                VoucherService.money(line.debit()), VoucherService.money(line.credit()), VoucherService.money(line.balance())));
        sections.add(lines);
        sections.add(balances(statement.opening(), statement.closing(), "Solde de clôture"));
        return vouchers.render("Relevé de compte partenaire", issued("RLP", statement.partner().getCode(), statement.closing()), sections, null, null,
                List.of(new VoucherService.Signature("Comptable", null, null), new VoucherService.Signature("Partenaire, pour accord", null, null)));
    }

    private VoucherService.Issued issued(String type, String code, BigDecimal closing) {
        Instant now = Instant.now();
        String number = type + "-" + code + "-" + STAMP.format(LocalDate.now());
        return new VoucherService.Issued(number, type, code, closing, null, actors.resolve("SYSTEM"), now, 0, null, null);
    }

    private static VoucherService.Section balances(BigDecimal opening, BigDecimal closing, String closingLabel) {
        return new VoucherService.Section("Soldes").pair("Solde d'ouverture", VoucherService.money(opening) + " XAF")
                .pair(closingLabel, VoucherService.money(closing) + " XAF");
    }

    private static String period(LocalDate from, LocalDate to) {
        return (from.getYear() <= 1900 ? "Depuis l'origine" : "Du " + DAY.format(from)) + " au " + DAY.format(to);
    }
}
