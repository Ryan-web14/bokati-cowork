package com.sni.bokaticowork.core.communication.mailService.service;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.communication.mailService.enums.EmailDeliveryStatus;
import com.sni.bokaticowork.core.communication.mailService.model.EmailDeliveryLog;
import com.sni.bokaticowork.core.communication.mailService.repository.EmailDeliveryLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
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
public class EmailRequeueWorker {

    private final EmailDeliveryLogRepository deliveryLogRepository;
    private final DefaultEmailSender emailSender;

    @Value("${bokati.mail.requeue.after-seconds:120}")
    private long requeueAfterSeconds;

    @Scheduled(fixedDelayString = "${bokati.mail.requeue.delay-ms:60000}")
    public void requeueStranded() {
        Instant before = Instant.now().minus(Duration.ofSeconds(Math.max(30, requeueAfterSeconds)));
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
}
