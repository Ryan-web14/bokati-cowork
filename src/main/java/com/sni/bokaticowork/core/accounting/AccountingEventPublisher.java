package com.sni.bokaticowork.core.accounting;

import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Publication d'un evenement comptable par un module metier (lot F2, plan section 3.2).
 *
 * <p>A appeler dans la transaction du fait metier : l'evenement part par l'outbox, si bien qu'il
 * n'existe que si le fait est enregistre, et qu'une comptabilite en panne ne bloque jamais
 * l'encaissement. Les modules metier ne dependent que de cette classe, jamais du module
 * {@code accounting}.</p>
 *
 * <p>La reference identifie le fait (numero de transaction, de facture, de mouvement) : un meme
 * couple (type, reference) ne produit qu'une piece, meme publie plusieurs fois.</p>
 */
@Component
@RequiredArgsConstructor
public class AccountingEventPublisher {

    /** Type de l'evenement outbox qui transporte les evenements comptables. */
    public static final String OUTBOX_EVENT_TYPE = "ACCOUNTING_EVENT";
    public static final String OUTBOX_AGGREGATE_TYPE = "ACCOUNTING";

    private final OutboxService outboxService;

    /**
     * @param eventType type comptable ({@code PAYMENT_RECEIVED}, {@code CASH_MOVEMENT_POSTED}...)
     * @param family    famille, pour l'interrupteur par famille ({@code CASH}, {@code SALES}...)
     * @param reference reference unique du fait
     * @param date      date du fait
     * @param data      donnees lues par les schemas d'ecriture
     */
    public void publish(String eventType, String family, String reference, LocalDate date, Map<String, ?> data) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", eventType);
        payload.put("family", family);
        payload.put("reference", reference);
        payload.put("date", date == null ? null : date.toString());
        payload.put("data", data == null ? Map.of() : data);
        outboxService.publish(OUTBOX_EVENT_TYPE, OUTBOX_AGGREGATE_TYPE, eventType + ":" + reference, payload);
    }
}
