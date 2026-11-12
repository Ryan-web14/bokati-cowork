package com.sni.bokaticowork.core.communication.mailService.service;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.communication.mailService.enums.EmailDeliveryStatus;
import com.sni.bokaticowork.core.communication.mailService.model.EmailDeliveryLog;
import com.sni.bokaticowork.core.communication.mailService.repository.EmailDeliveryLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import com.sni.bokaticowork.core.settings.platform.SettingTriggers;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Redepose les courriels que le broker n'a pas pris.
 *
 * <p>Un courriel est d'abord ecrit au journal, puis depose dans la file. Si le broker est
 * injoignable au depot, l'operation qui envoyait continue · une inscription ne tombe pas parce
 * que RabbitMQ est tombe · et le courriel reste QUEUED. C'est ce passage qui le rattrape, une
 * fois le broker revenu.</p>
 *
 * <p>Le delai avant redepot laisse au consommateur le temps de passer un courriel normal en
 * SENDING : un QUEUED plus vieux que ce delai n'est jamais arrive au broker.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailRequeueWorker implements SchedulingConfigurer {

    private static final String DELAY_KEY = "bokati.mail.requeue.delay-ms";
    private static final long DEFAULT_DELAY_MS = 60_000L;
    private static final String AFTER_KEY = "bokati.mail.requeue.after-seconds";

    private SettingTriggers triggers;
    private ObjectProvider<EmailRequeueWorker> self;
    private PlatformSettings platformSettings;

    private final EmailDeliveryLogRepository deliveryLogRepository;
    private final DefaultEmailSender emailSender;

    @Value("${bokati.mail.requeue.after-seconds:120}")
    private long requeueAfterSeconds;

    public void requeueStranded() {
        Instant before = Instant.now().minus(Duration.ofSeconds(Math.max(30, requeueAfterSeconds())));
        List<EmailDeliveryLog> stranded = deliveryLogRepository
                .findTop50ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(EmailDeliveryStatus.QUEUED, before);
        if (stranded.isEmpty()) {
            return;
        }
        int republished = 0;
        for (EmailDeliveryLog entry : stranded) {
            if (emailSender.republish(entry)) {
                republished++;
            } else {
                // Le broker n'est toujours pas la · inutile d'insister sur les suivants ce tour-ci.
                break;
            }
        }
        log.info("Courriels · {} sur {} redepose(s) dans la file", republished, stranded.size());
    }

    @Autowired
    void setSettingTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<EmailRequeueWorker> self) {
        this.self = self;
    }

    @Autowired(required = false)
    void setPlatformSettings(PlatformSettings platformSettings) {
        this.platformSettings = platformSettings;
    }

    /** Planification relue a chaque passage : un changement de parametre vaut au passage suivant. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().requeueStranded(), triggers.fixedDelay(DELAY_KEY, DEFAULT_DELAY_MS));
    }

    private long requeueAfterSeconds() {
        return platformSettings == null ? requeueAfterSeconds : platformSettings.number(AFTER_KEY, requeueAfterSeconds);
    }
}
