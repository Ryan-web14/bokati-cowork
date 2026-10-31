package com.sni.bokaticowork.features.company.sharing.dto;

import com.sni.bokaticowork.features.company.sharing.model.EntitlementSharing;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Le partage d'un abonnement d'organisation entre ses collaborateurs · regles, places, consommation.
 */
public final class SharingDtos {

    private SharingDtos() {
    }

    /** La regle d'un droit · {@code defaultQuota} sert en QUOTA et QUOTA_AND_POOL, {@code seatCap} partout. */
    public record RuleRequest(@NotNull EntitlementSharing.Mode mode,
                              @PositiveOrZero BigDecimal defaultQuota,
                              @PositiveOrZero BigDecimal seatCap) {
    }

    public record MaxSeatsRequest(@PositiveOrZero Integer maxSeats) {
    }

    /** Part et plafond propres a une place sur un droit · vides = ceux de la regle. */
    public record AllowanceRequest(@NotBlank String entitlementCode,
                                   @PositiveOrZero BigDecimal quota,
                                   @PositiveOrZero BigDecimal cap) {
    }

    public record AssignSeatRequest(@NotBlank String memberId,
                                    @Valid List<AllowanceRequest> allowances) {
    }

    /** {@code allowances} remplace, droit par droit, ce qui est donne · les autres droits ne changent pas. */
    public record UpdateSeatRequest(@Valid List<AllowanceRequest> allowances) {
    }

    public record ReleaseSeatRequest(@Size(max = 500) String reason) {
    }

    public record SubscriptionSummary(String subscriptionNumber,
                                      String planName,
                                      String status,
                                      LocalDate periodStart,
                                      LocalDate periodEnd,
                                      Integer maxSeats,
                                      long activeSeats) {
    }

    /** Un droit de l'abonnement, vu du partage · ce qui est recu, distribue, pris. */
    public record EntitlementView(String entitlementCode,
                                  String entitlementName,
                                  String unit,
                                  EntitlementSharing.Mode mode,
                                  BigDecimal defaultQuota,
                                  BigDecimal seatCap,
                                  boolean unlimited,
                                  BigDecimal granted,
                                  BigDecimal remaining,
                                  /** Somme des parts nominatives des places actives. */
                                  BigDecimal distributedQuotas,
                                  /** QUOTA_AND_POOL · null sinon, ou sans limite. */
                                  BigDecimal poolSize,
                                  BigDecimal poolUsed) {
    }

    public record SeatEntitlement(String entitlementCode,
                                  BigDecimal quota,
                                  BigDecimal quotaUsed,
                                  BigDecimal quotaRemaining,
                                  BigDecimal cap,
                                  BigDecimal used) {
    }

    public record SeatView(Long id,
                           String memberId,
                           String memberName,
                           String status,
                           Instant assignedAt,
                           String assignedBy,
                           List<SeatEntitlement> entitlements) {
    }

    public record SharingView(String companyCustomerCode,
                              SubscriptionSummary subscription,
                              List<EntitlementView> entitlements,
                              List<SeatView> seats) {
    }

    /** Ce qu'un collaborateur peut prendre sur l'abonnement d'une organisation. */
    public record MySeat(String companyCustomerCode,
                         String companyName,
                         String subscriptionNumber,
                         Long seatId,
                         LocalDate periodStart,
                         LocalDate periodEnd,
                         List<MySeatEntitlement> entitlements) {
    }

    public record MySeatEntitlement(String entitlementCode,
                                    String entitlementName,
                                    String unit,
                                    EntitlementSharing.Mode mode,
                                    BigDecimal quota,
                                    BigDecimal quotaRemaining,
                                    BigDecimal cap,
                                    BigDecimal used,
                                    /** Ce qui reste a l'organisation sur ce droit · le pot. */
                                    BigDecimal companyRemaining,
                                    BigDecimal poolRemaining,
                                    boolean unlimited) {
    }

    /** Liberer une place · les reservations a venir sur ce droit sont signalees, pas annulees. */
    public record ReleaseResult(Long seatId, String memberId, List<String> futureBookings, String warning) {
    }
}
