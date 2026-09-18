package com.sni.bokaticowork.features.subscription.subscription.pass.service;

import com.sni.bokaticowork.features.subscription.notification.dto.CreateSubscriptionNotificationRequest;
import com.sni.bokaticowork.features.subscription.notification.enums.SubscriptionNotificationChannel;
import com.sni.bokaticowork.features.subscription.notification.enums.SubscriptionNotificationType;
import com.sni.bokaticowork.features.subscription.notification.service.SubscriptionNotificationService;
import com.sni.bokaticowork.features.subscription.repository.PassRepository;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.pass.enums.PassAlertType;
import com.sni.bokaticowork.features.subscription.subscription.pass.model.PassAlert;
import com.sni.bokaticowork.features.subscription.subscription.pass.repository.PassAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Previent avant qu'il ne soit trop tard.
 *
 * <p>Trois situations qu'un titulaire decouvrait au mauvais moment : un pass qui expire dans
 * quelques jours, un solde presque epuise, et un pass achete puis jamais utilise. Les deux
 * premieres coutent au client ; la troisieme coute a la relation, et c'est celle que personne ne
 * voyait · un pass jamais utilise est un client qui ne reviendra pas, et on l'apprend quand il est
 * parti.</p>
 *
 * <p>Rien n'est envoye ici : les alertes sont mises en file, et l'ordonnanceur de notification qui
 * existe deja les distribue. Ce service detecte et se souvient, il ne double pas l'infrastructure
 * d'envoi.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PassAlertService {

    private static final ZoneId APP_ZONE = ZoneId.of("Africa/Lagos");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int BATCH_SIZE = 200;

    private final PassRepository passRepository;
    private final PassAlertRepository alertRepository;
    private final SubscriptionNotificationService notificationService;

    @Value("${bokati.pass.alert.expiring-days:7}")
    private int expiringDays;

    @Value("${bokati.pass.alert.low-balance-ratio:0.2}")
    private String lowBalanceRatio;

    @Value("${bokati.pass.alert.unused-days:14}")
    private int unusedDays;

    /**
     * Les trois detections sont exposees separement, et chacune porte sa transaction.
     *
     * <p>Les fondre en une seule ferait qu'une erreur sur les pass inutilises annulerait les
     * alertes d'expiration deja posees, et le prochain reveil les reposerait toutes.</p>
     */
    @Transactional
    public int announceExpiring() {
        Instant now = Instant.now();
        Instant until = now.plus(Duration.ofDays(expiringDays));
        List<Pass> passes = passRepository.findExpiringSoon(now, until, BATCH_SIZE);
        int queued = 0;
        for (Pass pass : passes) {
            long days = daysUntil(now, pass.getValidUntil());
            String subject = days <= 1
                    ? "Votre pass " + pass.getName() + " expire demain"
                    : "Votre pass " + pass.getName() + " expire dans " + days + " jours";
            // L'echeance fait la clef : un pass renouvele porte une nouvelle date, et se reannonce.
            if (announce(pass, PassAlertType.EXPIRING, dayKey(pass.getValidUntil()),
                    SubscriptionNotificationType.PASS_EXPIRING, subject,
                    payload(pass, "validUntil", DAY.format(pass.getValidUntil().atZone(APP_ZONE))))) {
                queued++;
            }
        }
        return queued;
    }

    @Transactional
    public int announceLowBalance() {
        Instant now = Instant.now();
        BigDecimal ratio = new BigDecimal(lowBalanceRatio);
        List<Pass> passes = passRepository.findLowBalance(ratio, now, BATCH_SIZE);
        int queued = 0;
        for (Pass pass : passes) {
            int remaining = remaining(pass);
            String subject = remaining <= 1
                    ? "Il vous reste une utilisation sur votre pass " + pass.getName()
                    : "Il vous reste " + remaining + " utilisations sur votre pass " + pass.getName();
            // Le solde restant fait la clef : chaque palier franchi merite son propre message, et
            // une seule alerte pour toute la descente laisserait le titulaire sans rappel.
            if (announce(pass, PassAlertType.LOW_BALANCE, String.valueOf(remaining),
                    SubscriptionNotificationType.PASS_LOW_BALANCE, subject,
                    payload(pass, "remainingUses", String.valueOf(remaining)))) {
                queued++;
            }
        }
        return queued;
    }

    @Transactional
    public int announceUnused() {
        Instant now = Instant.now();
        Instant since = now.minus(Duration.ofDays(unusedDays));
        List<Pass> passes = passRepository.findUnused(since, now, BATCH_SIZE);
        int queued = 0;
        for (Pass pass : passes) {
            String subject = "Votre pass " + pass.getName() + " vous attend";
            // Une seule fois par pass · relancer chaque semaine quelqu'un qui n'est pas venu ne le
            // fait pas venir, cela le fait se desabonner.
            if (announce(pass, PassAlertType.UNUSED, "once",
                    SubscriptionNotificationType.PASS_UNUSED, subject,
                    payload(pass, "unusedSinceDays", String.valueOf(unusedDays)))) {
                queued++;
            }
        }
        return queued;
    }

    // -----------------------------------------------------------------------------------------

    /**
     * Met une alerte en file, si elle n'a pas deja ete annoncee.
     *
     * @return vrai si l'alerte vient d'etre annoncee
     */
    private boolean announce(Pass pass,
                               PassAlertType alertType,
                               String thresholdKey,
                               SubscriptionNotificationType notificationType,
                               String subject,
                               String payloadJson) {
        if (alertRepository.alreadyAnnounced(pass.getId(), alertType.name(), thresholdKey)) {
            return false;
        }
        try {
            var notification = notificationService.queue(new CreateSubscriptionNotificationRequest(
                    pass.getSubscription() == null ? null : pass.getSubscription().getSubscriptionNumber(),
                    pass.getOwnerType(),
                    pass.getOwnerCode(),
                    notificationType,
                    SubscriptionNotificationChannel.EMAIL,
                    pass.getOwnerCode(),
                    subject,
                    null,
                    payloadJson,
                    Instant.now()
            ));

            alertRepository.save(PassAlert.builder()
                    .pass(pass)
                    .alertType(alertType)
                    .thresholdKey(thresholdKey)
                    .notificationNumber(notification == null ? null : notification.notificationNumber())
                    .payloadJson(payloadJson)
                    .build());
            return true;
        } catch (Exception ex) {
            log.warn("Alerte {} non annoncee sur le pass {} : {}",
                    alertType, pass.getPassNumber(), ex.getMessage());
            return false;
        }
    }

    /**
     * Jours restants, comptes en jours de calendrier.
     *
     * <p>Une duree tronquee dit « dans 2 jours » pour un pass qui expire apres-demain a la meme
     * heure moins une minute. Le titulaire lit la date, compte lui-meme, et le message le
     * contredit · ce qu'il retient alors est que le systeme se trompe.</p>
     */
    private long daysUntil(Instant now, Instant dueDate) {
        return Math.max(0, ChronoUnit.DAYS.between(
                now.atZone(APP_ZONE).toLocalDate(),
                dueDate.atZone(APP_ZONE).toLocalDate()));
    }

    private int remaining(Pass pass) {
        if (pass.getMaxUses() == null) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, pass.getMaxUses() - (pass.getUsedCount() == null ? 0 : pass.getUsedCount()));
    }

    private String dayKey(Instant instant) {
        return instant.atZone(APP_ZONE).toLocalDate().toString();
    }

    private String payload(Pass pass, String key, String value) {
        return "{\"passNumber\":\"" + pass.getPassNumber() + "\""
                + ",\"passName\":\"" + escape(pass.getName()) + "\""
                + ",\"" + key + "\":\"" + escape(value) + "\"}";
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
