package com.sni.bokaticowork.features.company.membership.dto;

import com.sni.bokaticowork.features.company.enums.InvoiceLinkMode;
import com.sni.bokaticowork.features.company.enums.MemberLinkRole;
import com.sni.bokaticowork.features.company.enums.MemberLinkStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Les echanges du rattachement d'un membre a une organisation.
 */
public final class MembershipDtos {

    private MembershipDtos() {
    }

    /** {@code invoiceLinkMode} vide = le mode par defaut de l'organisation. */
    public record LinkMemberRequest(@NotBlank String memberId,
                                    @NotNull MemberLinkRole role,
                                    Instant startedAt,
                                    InvoiceLinkMode invoiceLinkMode,
                                    Boolean canSign,
                                    Boolean canCollectMail) {
    }

    /** Champs absents = inchanges · {@code useCompanyDefaultMode} efface le mode propre au lien. */
    public record UpdateLinkRequest(MemberLinkRole role,
                                    InvoiceLinkMode invoiceLinkMode,
                                    Boolean useCompanyDefaultMode,
                                    Boolean canSign,
                                    Boolean canCollectMail) {
    }

    public record EndLinkRequest(@NotBlank @Size(max = 500) String reason) {
    }

    /** Le mode par defaut de l'organisation · {@code mode} obligatoire. */
    public record CompanyModeRequest(@NotNull InvoiceLinkMode mode) {
    }

    /** Le mode d'un lien · {@code mode} vide et {@code useCompanyDefaultMode} vrai pour revenir au defaut. */
    public record LinkModeRequest(InvoiceLinkMode mode, Boolean useCompanyDefaultMode) {
    }

    public record LinkResponse(Long id,
                               String customerId,
                               String companyName,
                               String memberId,
                               String memberName,
                               String memberEmail,
                               MemberLinkRole role,
                               MemberLinkStatus status,
                               Instant startedAt,
                               Instant endedAt,
                               String endedReason,
                               InvoiceLinkMode invoiceLinkMode,
                               InvoiceLinkMode effectiveInvoiceLinkMode,
                               boolean canSign,
                               boolean canCollectMail,
                               /** Vrai quand la fin du lien a fait repasser le membre sur sa fiche personnelle. */
                               Boolean payerChanged,
                               String warning) {
    }

    /** Ce qu'un membre voit de ses organisations dans son portail. */
    public record MyCompany(String customerId,
                            String name,
                            MemberLinkRole role,
                            Instant startedAt,
                            InvoiceLinkMode effectiveInvoiceLinkMode,
                            /** Visible du gerant seulement · null pour un employe. */
                            InvoiceLinkMode companyDefaultMode) {
    }
}
