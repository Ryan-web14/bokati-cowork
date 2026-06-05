package com.sni.bokaticowork.features.contract.service.implementation;

import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.contract.enums.ContractStatus;
import com.sni.bokaticowork.features.contract.model.Contract;
import com.sni.bokaticowork.features.contract.repository.ContractRepository;
import com.sni.bokaticowork.features.contract.service.interfaces.ContractLifecycleAutomationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ContractLifecycleAutomationServiceImpl implements ContractLifecycleAutomationService {

    private final ContractRepository contractRepository;
    private final OutboxService outboxService;

    @Override
    public int processScheduledTransitions() {
        return autoActivateContracts() + autoExpireContracts();
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
