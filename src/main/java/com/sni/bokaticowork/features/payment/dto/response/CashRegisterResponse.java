package com.sni.bokaticowork.features.payment.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record CashRegisterResponse(
        String registerCode,
        String name,
        String locationCode,
        String businessEntityCode,
        String deviceCode,
        Boolean active,
        Boolean cashControlEnabled,
        /** Caisse tenue par le systeme · l'interface doit y masquer toute saisie. */
        Boolean systemManaged,
        /** Unique moyen de paiement admis · nul si la caisse les accepte tous. */
        com.sni.bokaticowork.features.payment.enums.PaymentMethod restrictedToMethod,
        BigDecimal maxCashAmount,
        String managerEmail,
        Instant createdAt,
        Instant updatedAt,
        com.sni.bokaticowork.features.payment.enums.CashRegisterType registerType,
        com.sni.bokaticowork.features.payment.enums.CashRegisterStatus status,
        String parentRegisterCode,
        Instant lastCountAt,
        BigDecimal lastCountAmount,
        /** Caisse par defaut creee par le systeme : recoit les paiements saisis sans session. */
        Boolean defaultRegister
) {
    /** Forme sans la caisse par defaut. */
    public CashRegisterResponse(String registerCode, String name, String locationCode, String businessEntityCode,
                                String deviceCode, Boolean active, Boolean cashControlEnabled, Boolean systemManaged,
                                com.sni.bokaticowork.features.payment.enums.PaymentMethod restrictedToMethod, BigDecimal maxCashAmount,
                                String managerEmail, Instant createdAt, Instant updatedAt,
                                com.sni.bokaticowork.features.payment.enums.CashRegisterType registerType,
                                com.sni.bokaticowork.features.payment.enums.CashRegisterStatus status, String parentRegisterCode,
                                Instant lastCountAt, BigDecimal lastCountAmount) {
        this(registerCode, name, locationCode, businessEntityCode, deviceCode, active, cashControlEnabled, systemManaged,
                restrictedToMethod, maxCashAmount, managerEmail, createdAt, updatedAt, registerType, status, parentRegisterCode,
                lastCountAt, lastCountAmount, Boolean.FALSE);
    }

    /** Forme historique, sans les champs du referentiel. */
    public CashRegisterResponse(String registerCode, String name, String locationCode, String businessEntityCode,
                                String deviceCode, Boolean active, Boolean cashControlEnabled, Boolean systemManaged,
                                com.sni.bokaticowork.features.payment.enums.PaymentMethod restrictedToMethod, BigDecimal maxCashAmount,
                                String managerEmail, Instant createdAt, Instant updatedAt) {
        this(registerCode, name, locationCode, businessEntityCode, deviceCode, active, cashControlEnabled, systemManaged,
                restrictedToMethod, maxCashAmount, managerEmail, createdAt, updatedAt, null, null, null, null, null, Boolean.FALSE);
    }
}
