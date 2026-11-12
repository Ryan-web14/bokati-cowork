package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.repository.CashSessionRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.CashDocumentPdfService;
import com.sni.bokaticowork.security.admin.role.repository.RoleUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Le rapport de cloture d'une session de caisse, envoye a l'administration, a la gerance et au
 * responsable de la caisse · a chaque cloture, par un caissier comme par le systeme.
 *
 * <p>Le rapport PDF (flux, statistiques, mouvements) part en piece jointe, une fois la cloture
 * validee en base : un rapport ne part pas pour une cloture annulee. Un echec d'envoi est
 * journalise, il n'annule rien.</p>
 */
@Slf4j
@Component
public class CashClosingReportMailer {

    /** Les roles qui recoivent le rapport · noms stockes sans le prefixe ROLE_. */
    static final List<String> RECIPIENT_ROLES = List.of("SUPER_ADMIN", "ADMIN", "MANAGER");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Africa/Brazzaville"));

    private final CashSessionRepository sessionRepository;
    private final CashDocumentPdfService pdfService;
    private final RoleUserRepository roleUserRepository;
    private final DefaultEmailSender emailSender;
    private final SpringTemplateEngine templateEngine;
    private final boolean enabled;
    private final String supervisorEmail;
    private final String adminAccessUrl;

    public CashClosingReportMailer(CashSessionRepository sessionRepository, CashDocumentPdfService pdfService,
                                   RoleUserRepository roleUserRepository, DefaultEmailSender emailSender,
                                   SpringTemplateEngine templateEngine,
                                   @Value("${bokati.payment.cash-session.closing-report.enabled:true}") boolean enabled,
                                   @Value("${bokati.payment.cash-session.supervisor-email:${bokati.support.manager-email:supportela@elleaose.com}}") String supervisorEmail,
                                   @Value("${app.admin-access-url:https://admin.elleaose.com}") String adminAccessUrl) {
        this.sessionRepository = sessionRepository;
        this.pdfService = pdfService;
        this.roleUserRepository = roleUserRepository;
        this.emailSender = emailSender;
        this.templateEngine = templateEngine;
        this.enabled = enabled;
        this.supervisorEmail = supervisorEmail;
        this.adminAccessUrl = adminAccessUrl;
    }

    private static final String CLOSING_REPORT_ENABLED_KEY = "bokati.payment.cash-session.closing-report.enabled";
    private static final String SUPERVISOR_EMAIL_KEY = "bokati.payment.cash-session.supervisor-email";
    private static final String MANAGER_EMAIL_KEY = "bokati.support.manager-email";
    private static final String ADMIN_ACCESS_URL_KEY = "app.admin-access-url";

    private PlatformSettings platformSettings;

    /** Valeurs relues a l'usage ; celles du constructeur servent de repli. */
    @Autowired(required = false)
    void setPlatformSettings(PlatformSettings platformSettings) {
        this.platformSettings = platformSettings;
    }

    private boolean enabled() {
        return platformSettings == null ? enabled : platformSettings.bool(CLOSING_REPORT_ENABLED_KEY, enabled);
    }

    private String supervisorEmail() {
        if (platformSettings == null) {
            return supervisorEmail;
        }
        // repli imbrique : adresse du responsable du support, comme dans le fichier de configuration
        return platformSettings.text(SUPERVISOR_EMAIL_KEY, platformSettings.text(MANAGER_EMAIL_KEY, supervisorEmail));
    }

    private String adminAccessUrl() {
        return platformSettings == null ? adminAccessUrl : platformSettings.text(ADMIN_ACCESS_URL_KEY, adminAccessUrl);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onFinalized(CashSessionFinalizedEvent event) {
        if (!enabled()) {
            return;
        }
        try {
            CashSession session = sessionRepository.findBySessionNumber(event.sessionNumber()).orElse(null);
            if (session == null) {
                return;
            }
            Set<String> recipients = recipients(session.getCashRegister());
            if (recipients.isEmpty()) {
                log.warn("Rapport de cloture {} · aucun destinataire", event.sessionNumber());
                return;
            }
            byte[] pdf = pdfService.generateSessionClosingReport(event.sessionNumber());
            String subject = subject(session, event);
            String html = html(session, event, subject);
            for (String to : recipients) {
                emailSender.sendHtmlEmailWithPdfAttachment(to, subject, html, "cloture-caisse-" + session.getSessionNumber() + ".pdf", pdf);
            }
            log.info("Rapport de cloture {} envoye a {} destinataire(s)", session.getSessionNumber(), recipients.size());
        } catch (Exception ex) {
            log.warn("Rapport de cloture {} non envoye : {}", event.sessionNumber(), ex.getMessage(), ex);
        }
    }

    /** Administration, gerance, et le responsable de la caisse · ou, sans lui, le superviseur. */
    public Set<String> recipients(CashRegister register) {
        Set<String> recipients = new LinkedHashSet<>();
        for (String role : RECIPIENT_ROLES) {
            roleUserRepository.findEnabledHolderEmails(role).stream()
                    .filter(StringUtils::hasText).map(email -> email.trim().toLowerCase(Locale.ROOT)).forEach(recipients::add);
        }
        String responsible = register == null ? null : register.getManagerEmail();
        if (StringUtils.hasText(responsible)) {
            recipients.add(responsible.trim().toLowerCase(Locale.ROOT));
        } else {
            String supervisor = supervisorEmail();
            if (StringUtils.hasText(supervisor)) {
                recipients.add(supervisor.trim().toLowerCase(Locale.ROOT));
            }
        }
        return recipients;
    }

    static String subject(CashSession session, CashSessionFinalizedEvent event) {
        String register = session.getCashRegister() == null ? "" : " " + session.getCashRegister().getName();
        return (event.bySystem() ? "Clôture automatique de caisse" : "Clôture de caisse") + register + " · " + session.getSessionNumber();
    }

    private String html(CashSession session, CashSessionFinalizedEvent event, String subject) {
        Context context = new Context(Locale.FRENCH);
        context.setVariable("subject", subject);
        context.setVariable("recipientName", "équipe");
        context.setVariable("message", message(session, event));
        context.setVariable("reference", session.getSessionNumber());
        context.setVariable("actionUrl", adminAccessUrl().stripTrailing() + "/payments/cash-registers/sessions/" + session.getSessionNumber());
        return templateEngine.process("email/generic-notification", context);
    }

    public static String message(CashSession session, CashSessionFinalizedEvent event) {
        StringBuilder text = new StringBuilder();
        text.append("La session ").append(session.getSessionNumber());
        if (session.getCashRegister() != null) {
            text.append(" de la caisse ").append(session.getCashRegister().getName());
        }
        text.append(", ouverte par ").append(session.getOpenedBy());
        if (session.getOpenedAt() != null) {
            text.append(" le ").append(DATE_TIME.format(session.getOpenedAt()));
        }
        text.append(", a été clôturée ");
        text.append(event.bySystem() ? "par le système" : "par " + session.getClosedBy());
        if (session.getClosedAt() != null) {
            text.append(" le ").append(DATE_TIME.format(session.getClosedAt()));
        }
        text.append(". Montant attendu : ").append(amount(session.getExpectedClosingAmount())).append(" XAF");
        if (session.getCountedClosingAmount() != null) {
            text.append(", compté : ").append(amount(session.getCountedClosingAmount())).append(" XAF");
            text.append(", écart : ").append(amount(session.getVarianceAmount())).append(" XAF");
        } else {
            text.append(", espèces non comptées");
        }
        text.append(".");
        if (StringUtils.hasText(event.cause())) {
            text.append(" Motif : ").append(event.cause()).append(".");
        }
        if (StringUtils.hasText(event.renewedAs())) {
            text.append(" La session ").append(event.renewedAs()).append(" a été ouverte à sa suite.");
        }
        text.append(" Le rapport de clôture est joint.");
        return text.toString();
    }

    private static String amount(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }
}
