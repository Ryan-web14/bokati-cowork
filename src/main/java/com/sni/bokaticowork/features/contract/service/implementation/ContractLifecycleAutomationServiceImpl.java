package com.sni.bokaticowork.features.contract.service.implementation;

import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.contract.enums.ContractRenewalType;
import com.sni.bokaticowork.features.contract.enums.ContractStatus;
import com.sni.bokaticowork.features.contract.model.Contract;
import com.sni.bokaticowork.features.contract.repository.ContractRepository;
import com.sni.bokaticowork.features.contract.service.interfaces.ContractLifecycleAutomationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ContractLifecycleAutomationServiceImpl implements ContractLifecycleAutomationService {

    private static final List<Integer> EXPIRY_ALERT_DAYS = List.of(30, 7);

    private final ContractRepository contractRepository;
    private final OutboxService outboxService;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository subscriptionRepository;

    @Override
    public int processScheduledTransitions() {
        // La reconduction passe avant l'expiration : elle cherche les contrats ACTIVE echus,
        // que l'expiration ferait passer en EXPIRED. L'ancien contrat expire ensuite dans le meme passage.
        int activated = autoActivateContracts();
        int renewed = autoRenewContracts();
        int expired = autoExpireContracts();
        return activated + renewed + expired + alertExpiringContracts();
    }

    private int autoActivateContracts() {
        int processed = 0;
        LocalDate today = LocalDate.now();
        for (Contract contract : contractRepository.findAllByStatusAndDeletedFalse(ContractStatus.SIGNED)) {
            LocalDate activationDate = contract.getEffectiveDate() != null ? contract.getEffectiveDate() : contract.getStartDate();
            if (activationDate != null && activationDate.isAfter(today)) {
                continue;
            }
            contract.setStatus(ContractStatus.ACTIVE);
            contract.setActivatedAt(Instant.now());
            contractRepository.save(contract);
            publishEvent("CONTRACT_AUTO_ACTIVATED", contract);
            processed++;
        }
        return processed;
    }

    /**
     * Le contrat d'un abonnement vit aussi longtemps que lui · sa date de fin suit les periodes
     * payees, par avenant. Il n'expire pas pendant qu'un renouvellement attend son reglement, et
     * ne se reconduit jamais par un second contrat. Il suit l'abonnement quand celui-ci prend fin.
     */
    private Set<String> contractsOfOpenSubscriptions() {
        return new HashSet<>(subscriptionRepository.findContractCodesOfOpenSubscriptions());
    }

    private int autoExpireContracts() {
        int processed = 0;
        LocalDate today = LocalDate.now();
        Set<String> carried = contractsOfOpenSubscriptions();
        List<Contract> contracts = contractRepository.findAllByStatusInAndEndDateBeforeAndDeletedFalse(
                List.of(ContractStatus.ACTIVE, ContractStatus.SUSPENDED, ContractStatus.AMENDED),
                today
        );
        for (Contract contract : contracts) {
            if (contract.getEndDate() == null || !contract.getEndDate().isBefore(today)
                    || carried.contains(contract.getContractCode())) {
                continue;
            }
            contract.setStatus(ContractStatus.EXPIRED);
            contractRepository.save(contract);
            publishEvent("CONTRACT_AUTO_EXPIRED", contract);
            processed++;
        }
        return processed;
    }

    private int autoRenewContracts() {
        int processed = 0;
        LocalDate today = LocalDate.now();
        List<Contract> expiring = contractRepository.findAllByStatusInAndEndDateBeforeAndDeletedFalse(
                List.of(ContractStatus.ACTIVE), today);
        Set<String> carried = contractsOfOpenSubscriptions();
        for (Contract contract : expiring) {
            if (contract.getRenewalType() != ContractRenewalType.AUTO_RENEW
                    || carried.contains(contract.getContractCode())) {
                continue;
            }
            if (contract.getEndDate() == null || !contract.getEndDate().isBefore(today)) {
                continue;
            }
            try {
                long durationDays = contract.getStartDate() != null && contract.getEndDate() != null
                        ? ChronoUnit.DAYS.between(contract.getStartDate(), contract.getEndDate())
                        : 30;
                Contract renewed = Contract.builder()
                        .contractCode(sequenceGenerator.next("CONTRACT", LocalDate.now()))
                        .title(contract.getTitle())
                        .description(contract.getDescription())
                        .templateCode(contract.getTemplateCode())
                        .ownerType(contract.getOwnerType())
                        .ownerId(contract.getOwnerId())
                        .ownerCode(contract.getOwnerCode())
                        .business(contract.getBusiness())
                        .renewalType(contract.getRenewalType())
                        .startDate(contract.getEndDate())
                        .endDate(contract.getEndDate().plusDays(durationDays))
                        .effectiveDate(contract.getEndDate())
                        .createdBy(0L)
                        .status(ContractStatus.DRAFT)
                        .renewedFromCode(contract.getContractCode())
                        .build();
                contractRepository.save(renewed);
                publishEvent("CONTRACT_AUTO_RENEWED", renewed);
                processed++;
            } catch (Exception ex) {
                log.warn("Failed to auto-renew contract {}: {}", contract.getContractCode(), ex.getMessage());
            }
        }
        return processed;
    }

    private int alertExpiringContracts() {
        int alerted = 0;
        LocalDate today = LocalDate.now();
        for (int days : EXPIRY_ALERT_DAYS) {
            LocalDate targetDate = today.plusDays(days);
            List<Contract> contracts = contractRepository.findAllByStatusAndDeletedFalse(ContractStatus.ACTIVE);
            for (Contract contract : contracts) {
                if (contract.getEndDate() == null || !contract.getEndDate().equals(targetDate)) {
                    continue;
                }
                if (contract.getRenewalType() == ContractRenewalType.AUTO_RENEW) {
                    continue;
                }
                // Skip if this contract has already been alerted at this threshold (or a closer one).
                // Without this the hourly worker re-publishes the alert every hour for the whole day.
                Integer alreadyAlerted = contract.getExpiryAlertSentDays();
                if (alreadyAlerted != null && alreadyAlerted <= days) {
                    continue;
                }
                contract.setExpiryAlertSentDays(days);
                contractRepository.save(contract);
                HashMap<String, Object> payload = new HashMap<>();
                payload.put("contractCode", contract.getContractCode());
                payload.put("ownerType", contract.getOwnerType());
                payload.put("ownerCode", contract.getOwnerCode());
                payload.put("endDate", contract.getEndDate().toString());
                payload.put("daysUntilExpiry", days);
                payload.put("renewalType", contract.getRenewalType());
                outboxService.publish("CONTRACT_EXPIRY_ALERT", "CONTRACT", contract.getContractCode(), payload);
                alerted++;
            }
        }
        return alerted;
    }

    private void publishEvent(String eventType, Contract contract) {
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("contractCode", contract.getContractCode());
        payload.put("ownerType", contract.getOwnerType());
        payload.put("ownerCode", contract.getOwnerCode());
        payload.put("status", contract.getStatus());
        String docCode = StringUtils.hasText(contract.getSignedDocumentCode())
                ? contract.getSignedDocumentCode()
                : contract.getDraftDocumentCode();
        if (StringUtils.hasText(docCode)) {
            payload.put("documentCode", docCode);
        }
        outboxService.publish(eventType, "CONTRACT", contract.getContractCode(), payload);
    }
}
