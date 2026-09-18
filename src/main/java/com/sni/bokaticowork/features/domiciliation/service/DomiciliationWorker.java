package com.sni.bokaticowork.features.domiciliation.service;

import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.domiciliation.model.DomiciliationContract;
import com.sni.bokaticowork.features.domiciliation.repository.DomiciliationContractRepository;
import com.sni.bokaticowork.features.payment.service.support.TransactionContextResolver;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
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
public class DomiciliationWorker {

    private final DomiciliationContractRepository contractRepository;
    private final MailItemService mailItemService;
    private final TransactionContextResolver contextResolver;
    private final OutboxService outboxService;

    @Value("${bokati.domiciliation.renewal-reminder-days:30}")
    private int reminderDays;

    /** Attestations qui expirent dans la fenetre · une relance, avec la date. */
    @Scheduled(cron = "${bokati.domiciliation.reminder-cron:0 20 7 * * *}")
    @Transactional(readOnly = true)
    public void remindCertificateRenewals() {
        LocalDate today = LocalDate.now();
        List<DomiciliationContract> expiring = contractRepository.findCertificatesExpiringBetween(today, today.plusDays(Math.max(1, reminderDays)));
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
    @Scheduled(cron = "${bokati.domiciliation.mail-cron:0 30 7 * * *}")
    public void chaseMail() {
        int chased = mailItemService.chaseOverdue();
        if (chased > 0) {
            log.info("Domiciliation · {} pli(s) au-dela du delai de garde relance(s)", chased);
        }
    }

    /** Contrats a terme echu sans renouvellement · ils expirent, ils ne restent pas actifs par oubli. */
    @Scheduled(cron = "${bokati.domiciliation.expiry-cron:0 40 7 * * *}")
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
}
