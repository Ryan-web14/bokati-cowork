package com.sni.bokaticowork.core.audit.alert;

import com.sni.bokaticowork.core.communication.whatsapp.WhatsAppMessageSender;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.notification.dto.request.SendNotificationRequest;
import com.sni.bokaticowork.features.notification.enums.NotificationChannel;
import com.sni.bokaticowork.features.notification.enums.NotificationRecipientType;
import com.sni.bokaticowork.features.notification.service.interfaces.NotificationService;
import com.sni.bokaticowork.features.portal.notification.service.AdminInAppNotifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Envoi d'une alerte a son ouverture (lot A6), par les canaux choisis
 * ({@code bokati.audit.alerts.channels}) : dans l'application (bandeau en direct et notification aux
 * administrateurs choisis), par courriel (modele {@code ADMIN_ALERT}, modifiable dans l'editeur de
 * courriels) et par WhatsApp (modele {@code alerte_centre_audit}, a faire approuver par Meta). Un
 * canal en echec n'empeche pas les autres ; le resultat de chaque envoi est garde sur l'alerte.
 */
@Slf4j
@Component
public class AlertDispatcher {

    static final String CHANNELS_KEY = "bokati.audit.alerts.channels";
    static final String EMAILS_KEY = "bokati.audit.alerts.email-recipients";
    static final String PHONES_KEY = "bokati.audit.alerts.whatsapp-recipients";
    static final String WHATSAPP_PURPOSE = "AUDIT_ALERT";

    private final PlatformSettings settings;
    private final ObjectProvider<AdminInAppNotifier> inApp;
    private final ObjectProvider<NotificationService> notifications;
    private final ObjectProvider<WhatsAppMessageSender> whatsapp;

    public AlertDispatcher(PlatformSettings settings, ObjectProvider<AdminInAppNotifier> inApp,
                           ObjectProvider<NotificationService> notifications, ObjectProvider<WhatsAppMessageSender> whatsapp) {
        this.settings = settings;
        this.inApp = inApp;
        this.notifications = notifications;
        this.whatsapp = whatsapp;
    }

    /** Envoie l'alerte ; rend le compte rendu des envois ({@code APP ok, EMAIL 2/2, WHATSAPP echec ...}). */
    public String dispatch(long id, AlertRule rule, String message) {
        Set<String> channels = list(settings.text(CHANNELS_KEY, "APP,EMAIL,WHATSAPP")).stream()
                .map(c -> c.toUpperCase(Locale.ROOT)).collect(Collectors.toSet());
        List<String> emails = list(settings.text(EMAILS_KEY, ""));
        List<String> phones = list(settings.text(PHONES_KEY, ""));
        String reference = "ALR-" + id;
        String subject = "[" + ("CRITICAL".equals(rule.severity()) ? "Critique" : "Attention") + "] " + rule.title();
        List<String> report = new ArrayList<>();

        if (channels.contains("APP")) {
            report.add("APP " + inApp(reference, rule, subject, message, emails));
        }
        if (channels.contains("EMAIL")) {
            report.add("EMAIL " + email(reference, rule, subject, message, emails));
        }
        if (channels.contains("WHATSAPP")) {
            report.add("WHATSAPP " + whatsapp(reference, rule, message, phones));
        }
        return String.join(", ", report);
    }

    private String inApp(String reference, AlertRule rule, String subject, String message, List<String> emails) {
        AdminInAppNotifier notifier = inApp.getIfAvailable();
        if (notifier == null) {
            return "indisponible";
        }
        try {
            notifier.broadcastAlert(rule.name(), "AUDIT", subject, message, rule.severity(), "{\"reference\":\"" + reference + "\"}");
            for (String email : emails) {
                notifier.notify("AUDIT_ALERT", "AUDIT_ALERT", reference, email, subject, payload(reference, rule, subject, message));
            }
            return "ok";
        } catch (RuntimeException exception) {
            log.warn("Alerte {} : envoi dans l'application en echec : {}", reference, exception.getMessage());
            return "echec";
        }
    }

    private String email(String reference, AlertRule rule, String subject, String message, List<String> emails) {
        NotificationService service = notifications.getIfAvailable();
        if (service == null || emails.isEmpty()) {
            return emails.isEmpty() ? "aucun destinataire" : "indisponible";
        }
        int sent = 0;
        for (String email : emails) {
            try {
                service.send(new SendNotificationRequest("ADMIN_ALERT", "AUDIT_ALERT", reference, NotificationChannel.EMAIL,
                        NotificationRecipientType.ADMIN, null, email, "Administrateur", subject, "ADMIN_ALERT", null,
                        payload(reference, rule, subject, message), null));
                sent++;
            } catch (RuntimeException exception) {
                log.warn("Alerte {} : courriel a {} en echec : {}", reference, email, exception.getMessage());
            }
        }
        return sent + "/" + emails.size();
    }

    private String whatsapp(String reference, AlertRule rule, String message, List<String> phones) {
        WhatsAppMessageSender sender = whatsapp.getIfAvailable();
        if (sender == null || phones.isEmpty()) {
            return phones.isEmpty() ? "aucun destinataire" : "indisponible";
        }
        int sent = 0;
        String error = null;
        for (String phone : phones) {
            try {
                sender.send(phone, WHATSAPP_PURPOSE, List.of("équipe", rule.title(), cut(message, 900)), null, reference);
                sent++;
            } catch (RuntimeException exception) {
                error = exception.getMessage();
                log.warn("Alerte {} : WhatsApp au {} en echec : {}", reference, phone, exception.getMessage());
            }
        }
        return sent + "/" + phones.size() + (error == null ? "" : " (" + cut(error, 200) + ")");
    }

    private static Map<String, Object> payload(String reference, AlertRule rule, String subject, String message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("subject", subject);
        payload.put("preheader", rule.title());
        payload.put("message", message);
        payload.put("eventType", rule.name());
        payload.put("aggregateType", "AUDIT_ALERT");
        payload.put("aggregateId", reference);
        payload.put("reference", reference);
        return payload;
    }

    static List<String> list(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split("[,;\\s]+")).map(String::trim).filter(StringUtils::hasText).distinct().toList();
    }

    private static String cut(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
