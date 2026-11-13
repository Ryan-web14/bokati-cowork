package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.utils.validation.EmailRecipients;
import com.sni.bokaticowork.features.billing.dto.request.SendStatementRequest;
import com.sni.bokaticowork.features.company.statement.dto.StatementDtos;
import com.sni.bokaticowork.features.company.statement.service.AccountStatementExporter;
import com.sni.bokaticowork.features.company.statement.service.AccountStatementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Locale;

/**
 * Releve d'un client en PDF et son envoi (lot B9) : organisation (CUSTOMER) ou membre (MEMBER),
 * sur une periode, par le releve de compte existant.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CustomerStatementService {

    private final AccountStatementService statements;
    private final AccountStatementExporter exporter;
    private final DefaultEmailSender emailSender;

    public StatementDtos.Statement statement(String customerType, String customerCode, LocalDate from, LocalDate until) {
        String type = customerType == null ? "" : customerType.trim().toUpperCase(Locale.ROOT);
        return switch (type) {
            case "CUSTOMER", "COMPANY", "ORGANIZATION" -> statements.company(customerCode, from, until);
            case "MEMBER" -> statements.member(customerCode, from, until);
            default -> throw new BadRequestException("Relevé disponible pour une organisation ou un membre");
        };
    }

    public byte[] pdf(String customerType, String customerCode, LocalDate from, LocalDate until) {
        return exporter.pdf(statement(customerType, customerCode, from, until));
    }

    public String fileName(String customerType, String customerCode, LocalDate from, LocalDate until) {
        return AccountStatementExporter.fileName(statement(customerType, customerCode, from, until), "pdf");
    }

    public boolean send(String customerType, String customerCode, SendStatementRequest request) {
        StatementDtos.Statement statement = statement(customerType, customerCode, request.from(), request.until());
        String note = StringUtils.hasText(request.message()) ? "<p>" + HtmlUtils.htmlEscape(request.message().trim()) + "</p>" : "";
        String html = "<p>Bonjour,</p>" + note + "<p>Vous trouverez en pièce jointe votre relevé de compte chez ELLE A OSE.</p>"
                + "<p>Cordialement,<br>ELLE A OSE</p>";
        return emailSender.sendHtmlEmailWithPdfAttachment(EmailRecipients.encode(request.to(), request.cc(), request.bcc()),
                "Votre relevé de compte", html, AccountStatementExporter.fileName(statement, "pdf"), exporter.pdf(statement)).join();
    }
}
