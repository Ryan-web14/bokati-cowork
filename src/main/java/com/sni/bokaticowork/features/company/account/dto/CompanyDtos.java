package com.sni.bokaticowork.features.company.account.dto;

import com.sni.bokaticowork.core.baseClasses.dto.request.AddressRequest;
import com.sni.bokaticowork.features.company.enums.IdentifierType;
import com.sni.bokaticowork.features.company.enums.OrganizationType;
import com.sni.bokaticowork.features.company.enums.VatRegime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Les echanges du centre de gestion des entreprises · une fiche en un appel, mise a jour par bloc.
 */
public final class CompanyDtos {

    private CompanyDtos() {
    }

    /** Un identifiant saisi · {@code label} n'est utile que pour {@code AUTRE}. */
    public record IdentifierRequest(@NotNull IdentifierType type,
                                    @Size(max = 120) String label,
                                    @NotBlank @Size(max = 150) String value) {
    }

    public record CreateCompanyRequest(
            @NotNull OrganizationType organizationType,
            @NotBlank @Size(max = 255) String name,
            /** Forme juridique precise (SARL, SA...) · utile pour une societe, facultative sinon. */
            String legalForm,
            String activity,
            @Valid List<IdentifierRequest> identifiers,
            String legalRepresentativeName,
            String legalRepresentativeTitle,
            VatRegime vatRegime,
            @Size(max = 500) String vatExemptionNote,
            @Email String email,
            @Email String billingEmail,
            @NotBlank String phone,
            String website,
            @Valid AddressRequest address,
            @Valid AddressRequest billingAddress,
            @PositiveOrZero Integer paymentTermsDays,
            @PositiveOrZero BigDecimal creditLimit,
            /** XAF par defaut. */
            @Size(min = 3, max = 3) String currencyCode) {
    }

    /** Champs absents = inchanges · {@code identifiers} remplace la liste entiere quand il est fourni. */
    public record UpdateLegalRequest(OrganizationType organizationType,
                                     @Size(max = 255) String name,
                                     String legalForm,
                                     String activity,
                                     @Valid List<IdentifierRequest> identifiers,
                                     String legalRepresentativeName,
                                     String legalRepresentativeTitle,
                                     VatRegime vatRegime,
                                     @Size(max = 500) String vatExemptionNote) {
    }

    /** {@code removeCreditLimit} retire le plafond · un plafond absent laisse l'actuel. */
    public record UpdateBillingRequest(@Email String billingEmail,
                                       @Valid AddressRequest billingAddress,
                                       @PositiveOrZero Integer paymentTermsDays,
                                       @PositiveOrZero BigDecimal creditLimit,
                                       Boolean removeCreditLimit) {
    }

    public record UpdateContactRequest(@Email String email,
                                       String phone,
                                       String website,
                                       @Valid AddressRequest address) {
    }

    public record BlockRequest(@NotBlank @Size(max = 500) String reason) {
    }

    public record LinkLegalProfileRequest(@NotBlank String businessCode) {
    }

    // ── Reponses ──────────────────────────────────────────────────────────────

    public record IdentifierResponse(IdentifierType type, String label, String value, String format) {
    }

    public record AddressResponse(String streetNumber, String streetName, String district, String city, String country) {
    }

    public record CompanyResponse(
            String customerId,
            String name,
            OrganizationType organizationType,
            String organizationTypeLabel,
            String legalForm,
            String activity,
            List<IdentifierResponse> identifiers,
            List<IdentifierType> suggestedIdentifiers,
            String legalRepresentativeName,
            String legalRepresentativeTitle,
            VatRegime vatRegime,
            String vatExemptionNote,
            String email,
            String billingEmail,
            String phone,
            String website,
            AddressResponse address,
            AddressResponse billingAddress,
            Integer paymentTermsDays,
            BigDecimal creditLimit,
            BigDecimal outstanding,
            String status,
            String blockedReason,
            String blockedBy,
            Instant blockedAt,
            String businessCode,
            long membersCount,
            int activeSubscriptions,
            Instant createdAt) {
    }

    public record CompanySummary(String customerId,
                                 String name,
                                 OrganizationType organizationType,
                                 String niu,
                                 String rccm,
                                 String email,
                                 String phone,
                                 String status,
                                 BigDecimal outstanding) {
    }
}
