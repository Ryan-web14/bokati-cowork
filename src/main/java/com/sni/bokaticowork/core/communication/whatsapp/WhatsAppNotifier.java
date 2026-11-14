package com.sni.bokaticowork.core.communication.whatsapp;

import com.sni.bokaticowork.core.communication.whatsapp.infobip.InfobipWhatsAppProperties;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * Messages WhatsApp automatiques envoyes au client en plus du courriel (reservation confirmee ou
 * modifiee, abonnement actif, facture soldee).
 *
 * <p>Un envoi ne bloque jamais l'operation qui le declenche : il part apres la validation de la
 * transaction, sur un autre fil, et un echec (Infobip absent, modele non approuve, numero invalide)
 * est seulement journalise. Rien ne part si WhatsApp n'est pas configure, si le parametre
 * {@value #SETTING} est a faux, ou s'il n'y a pas de numero.</p>
 */
@Slf4j
@Component
public class WhatsAppNotifier {

    /** Interrupteur des messages automatiques ; les envois faits a la main depuis le back-office n'en dependent pas. */
    public static final String SETTING = "bokati.whatsapp.auto-notifications.enabled";

    private final ObjectProvider<WhatsAppMessageSender> sender;
    private final InfobipWhatsAppProperties properties;
    private final ObjectProvider<PlatformSettings> settings;
    private final Executor executor;

    public WhatsAppNotifier(ObjectProvider<WhatsAppMessageSender> sender, InfobipWhatsAppProperties properties,
                            ObjectProvider<PlatformSettings> settings, @Qualifier("taskExecutor") Executor executor) {
        this.sender = sender;
        this.properties = properties;
        this.settings = settings;
        this.executor = executor;
    }

    /** Vrai si un message automatique peut partir : Infobip active avec un expediteur, et l'interrupteur ouvert. */
    public boolean active() {
        if (!properties.isEnabled() || !StringUtils.hasText(properties.getSender())) {
            return false;
        }
        PlatformSettings current = settings.getIfAvailable();
        return current == null || current.bool(SETTING, true);
    }

    /**
     * Envoie le modele approuve de cet objet, apres la validation de la transaction en cours s'il y
     * en a une.
     *
     * @param phone           numero du destinataire ; vide, rien ne part
     * @param purpose         objet du modele (par exemple {@code BOOKING_CONFIRMED})
     * @param values          variables du modele, dans l'ordre
     * @param reference       reference metier pour le journal des envois
     * @param buttonParameter suffixe du lien du bouton du modele, s'il en a un
     */
    public void notify(String phone, String purpose, List<String> values, String reference, String buttonParameter) {
        if (!StringUtils.hasText(phone) || !active()) {
            return;
        }
        List<String> cleaned = values == null ? List.of() : values.stream().map(WhatsAppNotifier::clean).toList();
        String to = phone.trim();
        Runnable task = () -> send(to, purpose, cleaned, reference, buttonParameter);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    submit(task, purpose, reference);
                }
            });
        } else {
            submit(task, purpose, reference);
        }
    }

    private void submit(Runnable task, String purpose, String reference) {
        try {
            executor.execute(task);
        } catch (RejectedExecutionException ex) {
            log.warn("WhatsApp {} pour {} non envoye : file d'envoi pleine", purpose, reference);
        }
    }

    void send(String phone, String purpose, List<String> values, String reference, String buttonParameter) {
        WhatsAppMessageSender current = sender.getIfAvailable();
        if (current == null) {
            return;
        }
        try {
            WhatsAppMessageSender.Result result = current.send(phone, purpose, values, null, reference, buttonParameter);
            log.info("WhatsApp {} envoye pour {} (message {}, statut {})", purpose, reference, result.messageId(), result.statusName());
        } catch (RuntimeException ex) {
            log.warn("WhatsApp {} pour {} non envoye : {}", purpose, reference, ex.getMessage());
        }
    }

    /**
     * Variable acceptee par Meta : jamais vide, sans saut de ligne ni tabulation, sans plus de
     * quatre espaces de suite.
     */
    static String clean(String value) {
        if (!StringUtils.hasText(value)) {
            return "-";
        }
        return value.replaceAll("\\s+", " ").trim();
    }
}
