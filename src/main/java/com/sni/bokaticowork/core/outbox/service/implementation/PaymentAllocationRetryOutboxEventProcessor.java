package com.sni.bokaticowork.core.outbox.service.implementation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.outbox.model.OutboxEvent;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxEventProcessor;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import com.sni.bokaticowork.features.payment.service.support.PaymentAllocationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Rattrapage d'une imputation de paiement qui a echoue au moment de l'encaissement.
 * <p>
 * L'encaissement ne doit jamais etre annule parce que l'imputation a echoue : l'argent est
 * recu, la transaction doit passer SUCCEEDED. Mais avant, l'echec etait simplement journalise
 * et la facture restait impayee sans aucun mecanisme de reprise. Cet evenement remet
 * l'imputation dans la file outbox, qui la rejoue avec backoff jusqu'a succes.
 * <p>
 * Le rejeu est sur : {@code PaymentAllocationService.allocateOne} verifie l'existence d'une
 * imputation (transaction, facture) avant d'en creer une nouvelle.
 */
@Slf4j
@Component
public class PaymentAllocationRetryOutboxEventProcessor implements OutboxEventProcessor {

    private static final String EVENT_TYPE = "PAYMENT_ALLOCATION_RETRY";

    private final ObjectMapper objectMapper;
    private final PaymentTransactionRepository transactionRepository;
    private final PaymentAllocationService allocationService;

    public PaymentAllocationRetryOutboxEventProcessor(
            ObjectMapper objectMapper,
            @Lazy PaymentTransactionRepository transactionRepository,
            @Lazy PaymentAllocationService allocationService) {
        this.objectMapper = objectMapper;
        this.transactionRepository = transactionRepository;
        this.allocationService = allocationService;
    }

    @Override
    public boolean supports(OutboxEvent event) {
        return EVENT_TYPE.equals(event.getEventType());
    }

    @Override
    public void process(OutboxEvent event) {
        String transactionNumber = readTransactionNumber(event);
        PaymentTransaction transaction = transactionRepository.findByTransactionNumber(transactionNumber)
                .orElseThrow(() -> new IllegalStateException(
                        EVENT_TYPE + " · transaction introuvable : " + transactionNumber));

        allocationService.allocateIfBillingDocument(transaction);
        log.info("Imputation rattrapee pour la transaction {}", transactionNumber);
    }

    private String readTransactionNumber(OutboxEvent event) {
        JsonNode payload;
        try {
            payload = objectMapper.readTree(event.getPayload());
        } catch (Exception ex) {
            throw new IllegalStateException("Payload " + EVENT_TYPE + " illisible", ex);
        }
        String transactionNumber = payload.path("transactionNumber").asText(null);
        if (transactionNumber == null || transactionNumber.isBlank()) {
            throw new IllegalStateException(
                    EVENT_TYPE + " · transactionNumber absent du payload : " + event.getPayload());
        }
        return transactionNumber;
    }
}
