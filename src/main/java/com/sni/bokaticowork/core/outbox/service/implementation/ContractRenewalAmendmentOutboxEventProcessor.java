package com.sni.bokaticowork.core.outbox.service.implementation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.outbox.model.OutboxEvent;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxEventProcessor;
import com.sni.bokaticowork.features.contract.dto.response.ContractAmendmentResponse;
import com.sni.bokaticowork.features.contract.service.interfaces.ContractAmendmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Le renouvellement paye d'un abonnement prolonge son contrat, par un avenant du meme contrat.
 *
 * <p>L'avenant est applique aussitot : la reconduction est prevue au contrat, elle n'appelle ni
 * examen ni signature. Son PDF suit ; un PDF manque ne remet pas la prolongation en cause, il se
 * regenere depuis la fiche de l'avenant.</p>
 */
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

        String contractCode = payload.path("contractCode").asText(null);
        String subscriptionNumber = payload.path("subscriptionNumber").asText("?");
        LocalDate newStart = date(payload, "newPeriodStart");
        LocalDate newEnd = date(payload, "newPeriodEnd");

        if (!StringUtils.hasText(contractCode) || newStart == null || newEnd == null) {
            throw new IllegalStateException(
                    "CONTRACT_RENEWAL_AMENDMENT_REQUESTED payload incomplet: " + event.getPayload());
        }

        Optional<ContractAmendmentResponse> amendment = contractAmendmentService.applyRenewal(contractCode,
                new ContractAmendmentService.RenewalTerms(subscriptionNumber, date(payload, "previousPeriodStart"),
                        date(payload, "previousPeriodEnd"), newStart, newEnd, date(payload, "nextBillingDate")));
        if (amendment.isEmpty()) {
            log.info("Renouvellement de {} · contrat {} sans avenant", subscriptionNumber, contractCode);
            return;
        }
        String amendmentCode = amendment.get().getCode();
        log.info("Avenant de renouvellement {} applique au contrat {} ({})", amendmentCode, contractCode, subscriptionNumber);
        try {
            contractAmendmentService.generatePdf(amendmentCode, null);
        } catch (RuntimeException ex) {
            log.warn("Avenant {} · PDF non genere, a relancer depuis sa fiche : {}", amendmentCode, ex.getMessage());
        }
    }

    private static LocalDate date(JsonNode payload, String field) {
        String value = payload.path(field).asText(null);
        return StringUtils.hasText(value) ? LocalDate.parse(value) : null;
    }

    private JsonNode readPayload(OutboxEvent event) {
        try {
            return objectMapper.readTree(event.getPayload());
        } catch (Exception ex) {
            throw new IllegalStateException("Impossible de parser CONTRACT_RENEWAL_AMENDMENT_REQUESTED payload", ex);
        }
    }
}
