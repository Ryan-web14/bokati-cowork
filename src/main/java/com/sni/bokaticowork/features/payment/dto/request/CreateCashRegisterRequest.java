package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

/**
 * Creation d'une caisse.
 *
 * <p>Seuls le libelle et le type sont a choisir ; le reste est deduit ou herite : organisation et
 * caisse mere du site, plafonds et moyens du type de caisse (voir
 * {@code GET /cash-registers/form-defaults?context=REGISTER_CREATE}).</p>
 */
public record CreateCashRegisterRequest(
        @NotBlank String name,
        String locationCode,
        String businessEntityCode,
        String deviceCode,
        Boolean cashControlEnabled,
        BigDecimal maxCashAmount,
        /** Garde en lecture ; utiliser {@code alertRecipients}. */
        String managerEmail,
        com.sni.bokaticowork.features.payment.enums.CashRegisterType registerType,
        String parentRegisterCode,
        List<com.sni.bokaticowork.features.payment.enums.PaymentMethod> acceptedMethods,
        List<String> acceptedCurrencies,
        com.sni.bokaticowork.features.payment.enums.CashCapBehavior capBehavior,
        BigDecimal maxOutPerOperation,
        BigDecimal maxOutPerDay,
        BigDecimal standardFloat,
        List<String> alertRecipients,
        /** Titulaire (courriel) ; facultatif. */
        String holderEmail,
        /** Remplacants (courriels) dans l'ordre de preference ; facultatif. */
        List<String> backupEmails,
        List<CashRegisterScheduleRequest> schedules
) {
    /** Forme historique : une caisse de guichet sans autre precision. */
    public CreateCashRegisterRequest(String name, String locationCode, String businessEntityCode, String deviceCode,
                                     Boolean cashControlEnabled, BigDecimal maxCashAmount, String managerEmail) {
        this(name, locationCode, businessEntityCode, deviceCode, cashControlEnabled, maxCashAmount, managerEmail,
                null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
