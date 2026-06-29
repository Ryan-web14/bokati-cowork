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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ContractLifecycleAutomationServiceImpl implements ContractLifecycleAutomationService {

    private static final List<Integer> EXPIRY_ALERT_DAYS = List.of(30, 7);

    private final ContractRepository contractRepository;
    private final OutboxService outboxService;
    private final SequenceGeneratorFacade sequenceGenerator;

    @Override
    public int processScheduledTransitions() {
        return autoActivateContracts() + autoExpireContracts() + autoRenewContracts() + alertExpiringContracts();
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

    private int autoExpireContracts() {
        int processed = 0;
        LocalDate today = LocalDate.now();
        List<Contract> contracts = contractRepository.findAllByStatusInAndEndDateBeforeAndDeletedFalse(
                List.of(ContractStatus.ACTIVE, ContractStatus.SUSPENDED),
                today
        );
        for (Contract contract : contracts) {
            if (contract.getEndDate() == null || !contract.getEndDate().isBefore(today)) {
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
        for (Contract contract : expiring) {
            if (contract.getRenewalType() != ContractRenewalType.AUTO_RENEW) {
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
