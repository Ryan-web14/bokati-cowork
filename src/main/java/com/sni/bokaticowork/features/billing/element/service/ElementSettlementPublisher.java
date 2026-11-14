package com.sni.bokaticowork.features.billing.element.service;

import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Annonce qu'une facture vient de recevoir un paiement, quelle que soit la voie (paiement direct,
 * affectation tardive, rattachement d'une facture deja payee). Le traitement, par la boite de sortie,
 * applique aux elements que la facture regle les effets d'un paiement fait sur eux (lot R2).
 *
 * <p>Publie dans la transaction du paiement : rien ne part si le paiement est annule ; un echec des
 * effets est rejoue sans annuler le paiement.</p>
 */
@Component
@RequiredArgsConstructor
public class ElementSettlementPublisher {

    public static final String EVENT_TYPE = "BILLING_ELEMENTS_SETTLEMENT";
    /** Agregat propre : {@code BILLING_DOCUMENT} est deja pris par les notifications. */
    public static final String AGGREGATE_TYPE = "BILLING_ELEMENT";

    private final OutboxService outbox;

    public void publish(String documentNumber, String reason) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("documentNumber", documentNumber);
        payload.put("reason", reason);
        outbox.publish(EVENT_TYPE, AGGREGATE_TYPE, documentNumber, payload);
    }
}
