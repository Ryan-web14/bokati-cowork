package com.sni.bokaticowork.features.company.wallet.service;

import org.springframework.beans.factory.annotation.Autowired;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.communication.mailService.enums.EmailPriority;
import com.sni.bokaticowork.features.client.member.model.Member;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Locale;

/**
 * Les e-mails du portefeuille de l'organisation · demande de validation au gerant, decision au
 * collaborateur, code a usage unique a celui qui paie.
 *
 * <p>Envoyes une fois la transaction validee : un e-mail ne part pas pour une demande qui n'a pas
 * ete enregistree. Un echec d'envoi est journalise, il n'annule rien.</p>
 */
@Slf4j
@Component
public class CompanyWalletMailer {

    private final DefaultEmailSender emailSender;
    private final SpringTemplateEngine templateEngine;
    private final String portalUrl;

    private PlatformSettings platformSettings;

    @Autowired(required = false)
    void setPlatformSettings(PlatformSettings platformSettings) {
        this.platformSettings = platformSettings;
    }

    private static final String FRONTEND_URL_KEY = "app.frontend-url";

    private String portalUrl() {
        return platformSettings == null ? portalUrl : platformSettings.text(FRONTEND_URL_KEY, portalUrl);
    }

    public CompanyWalletMailer(DefaultEmailSender emailSender, SpringTemplateEngine templateEngine,
                               @Value("${app.frontend-url:https://elleaose.com}") String portalUrl) {
        this.emailSender = emailSender;
        this.templateEngine = templateEngine;
        this.portalUrl = portalUrl;
    }

    public void notify(Member recipient, String subject, String message, String reference, String portalPath) {
        send(recipient, subject, message, reference, portalPath, EmailPriority.NORMAL);
    }

    /** Une alerte a une adresse sans compte membre · l'equipe d'exploitation, par exemple. */
    public void notifyAddress(String email, String name, String subject, String message, String reference) {
        send(email, name, subject, message, reference, null, EmailPriority.NORMAL);
    }

    /** Le code a usage unique · prioritaire, il expire vite. */
    public void oneTimeCode(Member recipient, String code, String what, long validMinutes, String reference) {
        send(recipient, "Votre code de confirmation : " + code,
                "Pour confirmer " + what + ", saisissez le code " + code + ". Il est valable " + validMinutes
                        + " minutes et ne sert qu'une fois. Si vous n'êtes pas à l'origine de cette demande, ne le"
                        + " communiquez à personne et prévenez le gérant de votre organisation.",
                reference, null, EmailPriority.CRITICAL);
    }

    private void send(Member recipient, String subject, String message, String reference, String portalPath,
                      EmailPriority priority) {
        if (recipient == null) {
            return;
        }
        send(recipient.getEmail(), recipient.getDisplayName(), subject, message, reference, portalPath, priority);
    }

    private void send(String email, String name, String subject, String message, String reference, String portalPath,
                      EmailPriority priority) {
        if (!StringUtils.hasText(email)) {
            return;
        }
        String to = email.trim();
        Runnable dispatch = () -> {
            try {
                Context ctx = new Context(Locale.FRENCH);
                ctx.setVariable("subject", subject);
                ctx.setVariable("recipientName", name);
                ctx.setVariable("message", message);
                ctx.setVariable("reference", reference);
                String portalUrl = portalUrl();
                ctx.setVariable("actionUrl", StringUtils.hasText(portalPath) ? portalUrl.stripTrailing() + portalPath : portalUrl);
                emailSender.sendHtmlEmail(to, subject, templateEngine.process("email/generic-notification", ctx), priority);
            } catch (Exception ex) {
                log.warn("E-mail du portefeuille d'organisation non envoye a {} ({}) : {}", to, subject, ex.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch.run();
                }
            });
        } else {
            dispatch.run();
        }
    }
}
