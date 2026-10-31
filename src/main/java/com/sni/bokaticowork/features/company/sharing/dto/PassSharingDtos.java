package com.sni.bokaticowork.features.company.sharing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Les passes d'une organisation partages avec ses collaborateurs · acces, consommation.
 */
public final class PassSharingDtos {

    private PassSharingDtos() {
    }

    /**
     * Ouvre l'acces d'un collaborateur rattache au pass.
     *
     * @param maxUses   passages au plus pour cette personne · vide = ceux du pass
     * @param validFrom debut de l'acces · vide = tout de suite
     * @param validUntil fin de l'acces · vide = celle du pass
     */
    public record AddBeneficiaryRequest(@NotBlank String memberId,
                                        @Positive Integer maxUses,
                                        Instant validFrom,
                                        Instant validUntil) {
    }

    public record RevokeBeneficiaryRequest(@Size(max = 255) String reason) {
    }

    public record PassSummary(String passNumber,
                              String name,
                              String passType,
                              String status,
                              Instant validFrom,
                              Instant validUntil,
                              Integer maxUses,
                              Integer usedCount,
                              long beneficiaries) {
    }

    /** Un droit du pass · ce qui a ete achete et ce qui reste, pour tous les beneficiaires. */
    public record PassEntitlement(String entitlementCode,
                                  String name,
                                  String unit,
                                  boolean unlimited,
                                  BigDecimal granted,
                                  BigDecimal remaining) {
    }

    public record BeneficiaryConsumption(String entitlementCode, BigDecimal consumed) {
    }

    /** @param id numero court de l'acces */
    public record BeneficiaryView(Long id,
                                  String memberId,
                                  String memberName,
                                  boolean open,
                                  Integer maxUses,
                                  Integer usedCount,
                                  Instant validFrom,
                                  Instant validUntil,
                                  Instant addedAt,
                                  String addedBy,
                                  Instant revokedAt,
                                  String revokedReason,
                                  List<BeneficiaryConsumption> consumption) {
    }

    public record PassView(String companyCustomerCode,
                           PassSummary pass,
                           List<PassEntitlement> entitlements,
                           List<BeneficiaryView> beneficiaries) {
    }

    /** Ce que le collaborateur peut prendre sur un pass de son organisation. */
    public record MyPassAccess(String companyCustomerCode,
                               String companyName,
                               String passNumber,
                               String passName,
                               Instant validUntil,
                               Integer maxUses,
                               Integer usedCount,
                               List<PassEntitlement> entitlements) {
    }

    public record RevokeResult(Long id, String memberId, List<String> futureBookings, String warning) {
    }
}
