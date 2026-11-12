package com.sni.bokaticowork.features.accounting.service.support;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Notifications comptables (pieces en attente d'equilibrage, ecarts, alertes de continuite), par
 * courriel avec le modele generique, aux destinataires parametres et a l'auteur concerne.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountingNotifier {

    private final DefaultEmailSender emailSender;
    private final SpringTemplateEngine templateEngine;
    private final AccountingSettings settings;

    @Value("${app.admin-access-url:https://admin.elleaose.com}")
    private String adminAccessUrl;

    public void notifyAccountants(String subject, String message, String reference, String actionPath, String alsoTo) {
        Set<String> recipients = new LinkedHashSet<>(settings.notifyEmails());
        if (StringUtils.hasText(alsoTo) && alsoTo.contains("@")) {
            recipients.add(alsoTo.trim().toLowerCase(Locale.ROOT));
        }
        recipients.forEach(to -> send(to, subject, message, reference, actionPath));
    }

    private void send(String to, String subject, String message, String reference, String actionPath) {
        try {
            Context ctx = new Context(Locale.FRENCH);
            ctx.setVariable("subject", subject);
            ctx.setVariable("recipientName", "comptabilite");
            ctx.setVariable("message", message);
            ctx.setVariable("reference", reference);
            ctx.setVariable("actionUrl", StringUtils.hasText(actionPath) ? adminAccessUrl.stripTrailing() + actionPath : adminAccessUrl);
            emailSender.sendHtmlEmail(to, subject, templateEngine.process("email/generic-notification", ctx));
        } catch (MessagingException | RuntimeException exception) {
            log.warn("Notification comptable '{}' non envoyee a {}", subject, to, exception);
        }
    }
}
