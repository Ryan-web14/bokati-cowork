package com.sni.bokaticowork.features.payment.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

/** Fiche complete d'une caisse, avec ses affectations, horaires et postes. */
public record CashRegisterDetailResponse(
        String registerCode,
        String name,
        com.sni.bokaticowork.features.payment.enums.CashRegisterType registerType,
        com.sni.bokaticowork.features.payment.enums.CashRegisterStatus status,
        String statusReason,
        String locationCode,
        String businessEntityCode,
        String deviceCode,
        String parentRegisterCode,
        Boolean systemManaged,
        com.sni.bokaticowork.features.payment.enums.PaymentMethod restrictedToMethod,
        List<String> acceptedMethods,
        List<String> acceptedCurrencies,
        Boolean cashControlEnabled,
        BigDecimal maxCashAmount,
        com.sni.bokaticowork.features.payment.enums.CashCapBehavior capBehavior,
        BigDecimal maxOutPerOperation,
        BigDecimal maxOutPerDay,
        BigDecimal standardFloat,
        List<String> alertRecipients,
        String treasuryAccountNumber,
        String defaultJournalCode,
        Instant lastCountAt,
        BigDecimal lastCountAmount,
        /** Titulaire en vigueur maintenant, s'il y en a un. */
        String currentHolder,
        List<CashRegisterAssignmentResponse> assignments,
        List<Schedule> schedules,
        List<Device> devices,
        Instant createdAt,
        Instant updatedAt,
        String updatedBy,
        Instant closedAt
) {
    public record Schedule(Integer dayOfWeek, LocalTime opensAt, LocalTime closesAt) {
    }

    public record Device(String deviceCode, String label, com.sni.bokaticowork.features.payment.enums.CashDeviceStatus status,
                         Instant firstSeenAt, String firstSeenBy, Instant decidedAt, String decidedBy) {
    }
}
