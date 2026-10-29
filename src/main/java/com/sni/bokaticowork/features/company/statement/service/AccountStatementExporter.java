package com.sni.bokaticowork.features.company.statement.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.utils.BrandLogo;
import com.sni.bokaticowork.features.company.statement.dto.StatementDtos;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.nodes.Entities;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * L'etat de compte en PDF et en CSV · le meme contenu que la reponse JSON.
 */
@Component
@RequiredArgsConstructor
public class AccountStatementExporter {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SpringTemplateEngine templateEngine;
    private final Locale appLocale;
    private final BrandLogo brandLogo;

    public byte[] pdf(StatementDtos.Statement statement) {
        Context context = new Context(appLocale);
        context.setVariable("statement", statement);
        context.setVariable("fmt", new Formatter(appLocale));
        context.setVariable("logo", brandLogo.base64());
        context.setVariable("generatedAt", LocalDate.now());
        String html = templateEngine.process("billing/account-statement", context);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(html);
            jsoupDoc.outputSettings()
                    .syntax(OutputSettings.Syntax.xml)
                    .escapeMode(Entities.EscapeMode.xhtml)
                    .charset(StandardCharsets.UTF_8)
                    .prettyPrint(false);
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withW3cDocument(new W3CDom().fromJsoup(jsoupDoc), null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new BadRequestException("Impossible de produire l'état de compte en PDF", ex);
        }
    }

    public String csv(StatementDtos.Statement statement) {
        StringBuilder csv = new StringBuilder("﻿");
        csv.append("Document;Type;Statut;Emission;Echeance;Compte;Code compte;Organisation;Rattachement;Devise;Total;Regle;Reste du;Effet;Jours de retard;Dans la periode\n");
        for (StatementDtos.Line line : statement.lines()) {
            csv.append(cell(line.documentNumber())).append(';')
                    .append(cell(line.documentType())).append(';')
                    .append(cell(line.status())).append(';')
                    .append(line.issueDate() == null ? "" : line.issueDate()).append(';')
                    .append(line.dueDate() == null ? "" : line.dueDate()).append(';')
                    .append(cell(line.accountName())).append(';')
                    .append(cell(line.accountCode())).append(';')
                    .append(cell(line.companyName() != null ? line.companyName() : line.companyCustomerCode())).append(';')
                    .append(cell(line.companyLinkSource())).append(';')
                    .append(cell(line.currency())).append(';')
                    .append(line.totalAmount().toPlainString()).append(';')
                    .append(line.paidAmount().toPlainString()).append(';')
                    .append(line.balanceDue().toPlainString()).append(';')
                    .append(line.impact().toPlainString()).append(';')
                    .append(line.daysOverdue()).append(';')
                    .append(line.inPeriod() ? "oui" : "non").append('\n');
        }
        return csv.toString();
    }

    public static String fileName(StatementDtos.Statement statement, String extension) {
        return "etat-de-compte-" + statement.code() + "-" + statement.to() + "." + extension;
    }

    /** Une cellule CSV · guillemets doubles, et pas de formule interpretable par un tableur. */
    static String cell(String value) {
        if (value == null) {
            return "";
        }
        String safe = value;
        if (!safe.isEmpty() && "=+-@\t\r".indexOf(safe.charAt(0)) >= 0) {
            safe = "'" + safe;
        }
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    /** Montants et dates a la francaise pour le modele. */
    public static final class Formatter {
        private final Locale locale;

        Formatter(Locale locale) {
            this.locale = locale;
        }

        public String amount(BigDecimal value, String currency) {
            NumberFormat format = NumberFormat.getNumberInstance(locale);
            boolean whole = currency == null || "XAF".equalsIgnoreCase(currency) || "XOF".equalsIgnoreCase(currency);
            format.setMinimumFractionDigits(whole ? 0 : 2);
            format.setMaximumFractionDigits(whole ? 0 : 2);
            BigDecimal safe = value == null ? BigDecimal.ZERO : value.setScale(whole ? 0 : 2, RoundingMode.HALF_UP);
            return format.format(safe).replace(' ', ' ').replace(' ', ' ') + " " + (currency == null ? "XAF" : currency);
        }

        public String date(LocalDate value) {
            return value == null ? "" : DATE.format(value);
        }

        public String account(StatementDtos.AccountType type) {
            return switch (type) {
                case COMPANY -> "Organisation";
                case MEMBER -> "Membre";
                case PERSONAL -> "Compte personnel";
            };
        }

        public String status(String status) {
            if (status == null) return "";
            return switch (status) {
                case "PAID" -> "Réglée";
                case "PARTIALLY_PAID" -> "Partiellement réglée";
                case "OVERDUE" -> "En retard";
                case "CANCELLED", "VOIDED" -> "Annulée";
                case "REFUNDED" -> "Remboursée";
                case "WRITTEN_OFF" -> "Passée en perte";
                case "VALIDATED" -> "Validée";
                default -> "Émise";
            };
        }

        public String type(String type) {
            if (type == null) return "";
            return switch (type) {
                case "CREDIT_NOTE" -> "Avoir";
                case "PROFORMA_INVOICE" -> "Proforma";
                default -> "Facture";
            };
        }
    }
}
