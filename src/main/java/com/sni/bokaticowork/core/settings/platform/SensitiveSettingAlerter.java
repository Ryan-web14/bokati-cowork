package com.sni.bokaticowork.core.settings.platform;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.features.portal.notification.service.AdminInAppNotifier;
import com.sni.bokaticowork.security.admin.role.repository.RoleUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.util.HtmlUtils;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Signale aux super-administrateurs le changement d'un parametre sensible : il prend effet aussitot,
 * l'alerte dit lequel, par qui, de quelle valeur a quelle valeur et ce qu'il risque. L'envoi part
 * apres l'enregistrement : un changement annule ne previent personne. Un echec d'envoi est
 * journalise et ne bloque pas le changement.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SensitiveSettingAlerter {

    static final String ALERT_TYPE = "SENSITIVE_SETTING_CHANGED";
    private static final String MODULE = "SETTINGS";

    private final RoleUserRepository roleUserRepository;
    private final DefaultEmailSender emailSender;
    private final AdminInAppNotifier adminInAppNotifier;

    public void changed(String key, String label, String oldValue, String newValue, String actor) {
        if (!SensitiveSettings.isSensitive(key) || Objects.equals(oldValue, newValue)) {
            return;
        }
        Runnable send = () -> send(key, label, oldValue, newValue, actor);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }

    void send(String key, String label, String oldValue, String newValue, String actor) {
        String risk = SensitiveSettings.risk(key);
        String title = "Paramètre sensible modifié : " + (label != null ? label : key);
        String message = key + " : « " + shown(oldValue) + " » devient « " + shown(newValue) + " », par " + actor
                + ". Risque : " + risk + ".";
        log.warn("Parametre sensible {} modifie par {} : {} -> {}", key, actor, shown(oldValue), shown(newValue));
        adminInAppNotifier.broadcastAlert(ALERT_TYPE, MODULE, title, message, "WARNING", null);
        Set<String> recipients;
        try {
            recipients = new LinkedHashSet<>(roleUserRepository.findEnabledHolderEmails("SUPER_ADMIN"));
        } catch (RuntimeException ex) {
            log.warn("Destinataires de l'alerte sur {} introuvables : {}", key, ex.getMessage());
            return;
        }
        String html = "<p>Un paramètre sensible vient d'être modifié ; il est déjà en vigueur.</p>"
                + "<ul>"
                + "<li><b>Paramètre</b> : " + HtmlUtils.htmlEscape(label != null ? label : key) + " (" + HtmlUtils.htmlEscape(key) + ")</li>"
                + "<li><b>Ancienne valeur</b> : " + HtmlUtils.htmlEscape(shown(oldValue)) + "</li>"
                + "<li><b>Nouvelle valeur</b> : " + HtmlUtils.htmlEscape(shown(newValue)) + "</li>"
                + "<li><b>Modifié par</b> : " + HtmlUtils.htmlEscape(actor) + "</li>"
                + "<li><b>Risque</b> : " + HtmlUtils.htmlEscape(risk) + "</li>"
                + "</ul>"
                + "<p>Si ce changement n'est pas voulu, remettez la valeur précédente depuis les paramètres de la plateforme.</p>";
        for (String recipient : recipients) {
            try {
                emailSender.queueEmail(recipient, title, html, true, "PLATFORM_SETTING", key);
            } catch (RuntimeException ex) {
                log.warn("Alerte sur {} non envoyee a {} : {}", key, recipient, ex.getMessage());
            }
        }
    }

    private static String shown(String value) {
        return value == null || value.isEmpty() ? "(vide)" : value;
    }
}
