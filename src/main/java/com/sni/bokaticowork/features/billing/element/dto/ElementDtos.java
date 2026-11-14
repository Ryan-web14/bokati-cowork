package com.sni.bokaticowork.features.billing.element.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Vues du rattachement des paiements et des factures aux elements (lots R1 a R4). */
public final class ElementDtos {

    private ElementDtos() {
    }

    /**
     * Facture d'un element.
     *
     * @param origin {@code OWN} (produite par l'element) ou {@code LINKED} (rattachee par l'equipe)
     * @param activationMode mode d'activation du lien, pour une facture rattachee
     */
    public record Invoice(String documentNumber, String documentType, String status, String customerType, String customerCode,
                          String currency, BigDecimal totalAmount, BigDecimal balanceDue, LocalDate dueDate, String origin,
                          String activationMode) {
    }

    /** Element facture, avec son titulaire et son etat. */
    public record Element(String type, String code, String label, String status, String holderType, String holderCode,
                          LocalDate startDate, LocalDate endDate, BigDecimal totalAmount) {
    }

    /** Element regle par une facture. */
    public record Settled(String type, String code, String origin, String activationMode) {
    }

    /** Tiers trouve par la recherche, avec ses elements rattachables. */
    public record Party(String type, String code, String name, String email, String phone, boolean external, List<Element> elements) {
    }

    /** Rattachement d'une facture a un element. */
    public record LinkRequest(String type, String code, String activationMode, Boolean replaceOwnInvoice) {
    }
}
