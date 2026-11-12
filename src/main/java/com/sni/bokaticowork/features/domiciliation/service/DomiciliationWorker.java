package com.sni.bokaticowork.features.domiciliation.service;

import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.domiciliation.model.DomiciliationContract;
import com.sni.bokaticowork.features.domiciliation.repository.DomiciliationContractRepository;
import com.sni.bokaticowork.features.payment.service.support.TransactionContextResolver;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ce que la domiciliation fait sans qu'on la lance.
 *
 * <p>La relance avant echeance de l'attestation, parce qu'une attestation echue se decouvre le jour
 * ou on en a besoin. Et la garde du courrier, parce qu'un pli oublie au guichet finit par couter
 * de la place et de l'attention.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DomiciliationWorker implements SchedulingConfigurer {

    private static final String REMINDER_CRON_KEY = "bokati.domiciliation.reminder-cron";
    private static final String DEFAULT_REMINDER_CRON = "0 20 7 * * *";
    private static final String MAIL_CRON_KEY = "bokati.domiciliation.mail-cron";
    private static final String DEFAULT_MAIL_CRON = "0 30 7 * * *";
    private static final String EXPIRY_CRON_KEY = "bokati.domiciliation.expiry-cron";
    private static final String DEFAULT_EXPIRY_CRON = "0 40 7 * * *";
    private static final String REMINDER_DAYS_KEY = "bokati.domiciliation.renewal-reminder-days";

    private SettingTriggers triggers;
    private ObjectProvider<DomiciliationWorker> self;
    private PlatformSettings platformSettings;

    private final DomiciliationContractRepository contractRepository;
    private final MailItemService mailItemService;
    private final TransactionContextResolver contextResolver;
    private final OutboxService outboxService;

    @Value("${bokati.domiciliation.renewal-reminder-days:30}")
    private int reminderDays;

    /** Attestations qui expirent dans la fenetre · une relance, avec la date. */
    @Transactional(readOnly = true)
    public void remindCertificateRenewals() {
        LocalDate today = LocalDate.now();
        List<DomiciliationContract> expiring = contractRepository.findCertificatesExpiringBetween(today, today.plusDays(Math.max(1, reminderDays())));
        int sent = 0;
        for (DomiciliationContract contract : expiring) {
            Subscription subscription = contract.getSubscription();
            TransactionContextResolver.PartyView party = contextResolver.resolveParty(
                    subscription.getSubscriberType().name(), subscription.getSubscriberCode());
            if (!StringUtils.hasText(party.email())) {
                continue;
            }
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("recipientEmail", party.email());
            payload.put("recipientName", party.name());
            payload.put("recipientType", subscription.getSubscriberType().name());
            payload.put("recipientCode", subscription.getSubscriberCode());
            payload.put("subject", "Votre attestation de domiciliation expire le " + contract.getCertificateValidUntil());
            payload.put("templateCode", "DOMICILIATION_CERTIFICATE_EXPIRING");
            payload.put("contractNumber", contract.getContractNumber());
            payload.put("validUntil", contract.getCertificateValidUntil().toString());
            payload.put("scope", contract.getCertificateScope() == null ? "" : contract.getCertificateScope().name());
            outboxService.publish("DOMICILIATION_CERTIFICATE_EXPIRING", "DOMICILIATION",
                    contract.getContractNumber() + ":" + contract.getCertificateValidUntil(), payload);
            sent++;
        }
        if (sent > 0) {
            log.info("Domiciliation · {} relance(s) d'attestation envoyee(s)", sent);
        }
    }

    /** Courrier au-dela du delai de garde · relance et garde prolongee facturee. */
    public void chaseMail() {
        int chased = mailItemService.chaseOverdue();
        if (chased > 0) {
            log.info("Domiciliation · {} pli(s) au-dela du delai de garde relance(s)", chased);
        }
    }

    /** Contrats a terme echu sans renouvellement · ils expirent, ils ne restent pas actifs par oubli. */
    @Transactional
    public void expireEndedContracts() {
        LocalDate today = LocalDate.now();
        int expired = 0;
        for (DomiciliationContract contract : contractRepository.registry()) {
            if (contract.getStatus() == DomiciliationContract.Status.ACTIVE && contract.getEndDate() != null
                    && contract.getEndDate().isBefore(today)
                    && !Boolean.TRUE.equals(contract.getSubscription().getAutoRenew())) {
                contract.setStatus(DomiciliationContract.Status.EXPIRED);
                if (contract.getCertificateRevokedAt() == null && contract.getCertificateDocumentCode() != null) {
                    contract.setCertificateRevokedAt(java.time.Instant.now());
                    contract.setCertificateRevocationReason("Contrat arrivé à terme");
                }
                contractRepository.save(contract);
                expired++;
            }
        }
        if (expired > 0) {
            log.info("Domiciliation · {} contrat(s) arrive(s) a terme", expired);
        }
    }

    @Autowired
    void setSettingTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<DomiciliationWorker> self) {
        this.self = self;
    }

    @Autowired(required = false)
    void setPlatformSettings(PlatformSettings platformSettings) {
        this.platformSettings = platformSettings;
    }

    /** Planification relue a chaque passage : un changement de parametre vaut au passage suivant. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().remindCertificateRenewals(), triggers.cron(REMINDER_CRON_KEY, DEFAULT_REMINDER_CRON));
        registrar.addTriggerTask(() -> self.getObject().chaseMail(), triggers.cron(MAIL_CRON_KEY, DEFAULT_MAIL_CRON));
        registrar.addTriggerTask(() -> self.getObject().expireEndedContracts(), triggers.cron(EXPIRY_CRON_KEY, DEFAULT_EXPIRY_CRON));
    }

    private int reminderDays() {
        return platformSettings == null ? reminderDays : platformSettings.integer(REMINDER_DAYS_KEY, reminderDays);
    }
}
