package com.sni.bokaticowork.features.payment.dto.request;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Modification de la fiche d'une caisse ; seuls les champs fournis changent.
 *
 * <p>Chaque changement est historise (ancienne et nouvelle valeur, auteur, date, motif). Un
 * changement de plafond prend effet tout de suite ; un changement de compte comptable a la date
 * {@code effectiveAt}. Une liste vide efface (moyens acceptes, horaires, destinataires).</p>
 */
public record UpdateCashRegisterRequest(
        String name,
        String locationCode,
        String businessEntityCode,
        String parentRegisterCode,
        Boolean cashControlEnabled,
        BigDecimal maxCashAmount,
        List<com.sni.bokaticowork.features.payment.enums.PaymentMethod> acceptedMethods,
        List<String> acceptedCurrencies,
        com.sni.bokaticowork.features.payment.enums.CashCapBehavior capBehavior,
        BigDecimal maxOutPerOperation,
        BigDecimal maxOutPerDay,
        BigDecimal standardFloat,
        List<String> alertRecipients,
        String treasuryAccountNumber,
        String defaultJournalCode,
        List<CashRegisterScheduleRequest> schedules,
        Instant effectiveAt,
        String reason
) {
}
