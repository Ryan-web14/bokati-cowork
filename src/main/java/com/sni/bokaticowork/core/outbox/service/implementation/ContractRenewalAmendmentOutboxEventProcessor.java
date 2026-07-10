package com.sni.bokaticowork.core.outbox.service.implementation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.outbox.model.OutboxEvent;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxEventProcessor;
import com.sni.bokaticowork.features.contract.dto.request.ContractAmendmentVariableRequest;
import com.sni.bokaticowork.features.contract.dto.request.ProposeAmendmentRequest;
import com.sni.bokaticowork.features.contract.dto.response.ContractAmendmentResponse;
import com.sni.bokaticowork.features.contract.service.interfaces.ContractAmendmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
public class ContractRenewalAmendmentOutboxEventProcessor implements OutboxEventProcessor {

    private final ObjectMapper objectMapper;
    private final ContractAmendmentService contractAmendmentService;

    public ContractRenewalAmendmentOutboxEventProcessor(ObjectMapper objectMapper,
                                                        @Lazy ContractAmendmentService contractAmendmentService) {
        this.objectMapper = objectMapper;
        this.contractAmendmentService = contractAmendmentService;
    }

    @Override
    public boolean supports(OutboxEvent event) {
        return "CONTRACT_RENEWAL_AMENDMENT_REQUESTED".equals(event.getEventType());
    }

    @Override
    public void process(OutboxEvent event) {
        JsonNode payload = readPayload(event);

        String contractCode       = payload.path("contractCode").asText(null);
        String subscriptionNumber = payload.path("subscriptionNumber").asText("?");
        String newPeriodStart     = payload.path("newPeriodStart").asText(null);
        String newPeriodEnd       = payload.path("newPeriodEnd").asText(null);
        String nextBillingDate    = payload.path("nextBillingDate").asText(null);
        String previousPeriodStart = payload.path("previousPeriodStart").asText(null);
        String previousPeriodEnd   = payload.path("previousPeriodEnd").asText(null);

        if (!StringUtils.hasText(contractCode) || !StringUtils.hasText(newPeriodStart)) {
            throw new IllegalStateException(
                    "CONTRACT_RENEWAL_AMENDMENT_REQUESTED payload incomplet: " + event.getPayload());
        }

        LocalDate effectiveDate = LocalDate.parse(newPeriodStart);
        String description = String.format(
                "Renouvellement de l'abonnement %s · nouvelle période du %s au %s",
                subscriptionNumber, newPeriodStart, valueOrDash(newPeriodEnd));

        ProposeAmendmentRequest proposeRequest = new ProposeAmendmentRequest();
        proposeRequest.setDescription(description);
        proposeRequest.setEffectiveDate(effectiveDate);

        ContractAmendmentResponse amendment;
        try {
            amendment = contractAmendmentService.propose(contractCode, null, proposeRequest);
        } catch (BadRequestException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("avenant est déjà en cours")) {
                log.info("Renouvellement avenant ignoré pour contrat {} ({}): {}", contractCode, subscriptionNumber, ex.getMessage());
                return;
            }
            throw ex;
        }

        List<ContractAmendmentVariableRequest> variables = buildPeriodVariables(
                previousPeriodStart, previousPeriodEnd,
                newPeriodStart, newPeriodEnd, nextBillingDate);

        contractAmendmentService.setVariables(amendment.getCode(), variables);
        contractAmendmentService.submitForReview(amendment.getCode());

        log.info("Avenant de renouvellement {} créé pour contrat {} ({})",
                amendment.getCode(), contractCode, subscriptionNumber);
    }

    private List<ContractAmendmentVariableRequest> buildPeriodVariables(
            String prevStart, String prevEnd,
            String newStart, String newEnd, String nextBilling) {

        return List.of(
                variable("periode_debut",           prevStart,   newStart),
                variable("periode_fin",             prevEnd,     newEnd),
                variable("prochaine_facturation",   null,        valueOrDash(nextBilling))
        );
    }

    private ContractAmendmentVariableRequest variable(String key, String previousValue, String newValue) {
        ContractAmendmentVariableRequest req = new ContractAmendmentVariableRequest();
        req.setVariableKey(key);
        req.setPreviousValue(StringUtils.hasText(previousValue) ? previousValue : null);
        req.setNewValue(StringUtils.hasText(newValue) ? newValue : "-");
        return req;
    }

    private String valueOrDash(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }

    private JsonNode readPayload(OutboxEvent event) {
        try {
            return objectMapper.readTree(event.getPayload());
        } catch (Exception ex) {
            throw new IllegalStateException("Impossible de parser CONTRACT_RENEWAL_AMENDMENT_REQUESTED payload", ex);
        }
    }
}
