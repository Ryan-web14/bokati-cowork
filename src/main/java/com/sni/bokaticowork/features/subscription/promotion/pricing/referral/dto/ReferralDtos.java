package com.sni.bokaticowork.features.subscription.promotion.pricing.referral.dto;

import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.enums.ReferralProgramStatus;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.enums.ReferralQualificationRule;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.enums.ReferralStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Les echanges du parrainage : programmes (back-office), liens (back-office et espace client),
 * parrainages, et la page publique d'un lien.
 *
 * <p>Toutes les vues sont construites dans la transaction du service : les liens entre programme,
 * promotions et parrainages sont paresseux.</p>
 */
public final class ReferralDtos {

    private ReferralDtos() {
    }

    /**
     * Creer ou modifier un programme.
     *
     * <p>{@code referrerPromotionCode} : ce que recoit le parrain ; {@code refereePromotionCode} :
     * ce que recoit le filleul. Au moins l'une des deux pour activer. Une fois le programme active,
     * regle et promotions ne changent plus : les parrainages en cours ont ete promis avec elles.</p>
     */
    public record ProgramRequest(
            @NotBlank @Size(max = 100) String code,
            @NotBlank String name,
            String description,
            Instant validFrom,
            Instant validUntil,
            String referrerPromotionCode,
            String refereePromotionCode,
            ReferralQualificationRule qualificationRule,
            Integer qualificationDelayDays,
            Integer maxReferralsPerReferrer,
            String currency
    ) {}

    public record PromotionRef(String code, String name, String description) {}

    /** {@code referralsByStatus} : PENDING, QUALIFIED, REWARDED, REJECTED. */
    public record ProgramView(
            String code, String name, String description, ReferralProgramStatus status, boolean open,
            Instant validFrom, Instant validUntil,
            PromotionRef referrerPromotion, PromotionRef refereePromotion,
            ReferralQualificationRule qualificationRule, Integer qualificationDelayDays,
            Integer maxReferralsPerReferrer, String currency,
            long links, Map<String, Long> referralsByStatus,
            Instant createdAt, String createdBy, Instant updatedAt
    ) {}

    /** Demande d'un lien par le back-office, pour un parrain designe. */
    public record LinkRequest(@NotBlank String programCode, @NotBlank String referrerType, @NotBlank String referrerCode) {}

    /** Demande de son propre lien depuis l'espace client. */
    public record ClientLinkRequest(@NotBlank String programCode) {}

    /** {@code url} : le lien a partager, qui mene a l'inscription avec le code deja saisi. */
    public record LinkView(
            String code, String url, String programCode, String programName, boolean programOpen,
            String referrerType, String referrerCode,
            int clickCount, int signupCount, boolean active, Instant createdAt
    ) {}

    public record ReferralView(
            String referralNumber, String programCode, String programName, String linkCode,
            String referrerType, String referrerCode, String refereeType, String refereeCode,
            ReferralStatus status, Instant registeredAt, Instant qualifiedAt, Instant rejectedAt, String rejectionReason,
            boolean referrerRewardGranted, BigDecimal referrerRewardAmount, Instant referrerRewardedAt,
            boolean refereeRewardGranted, BigDecimal refereeRewardAmount, Instant refereeRewardedAt,
            String currency
    ) {}

    /**
     * Un parrainage vu par son parrain : le filleul n'y figure que par son prenom et l'initiale de
     * son nom, et seule la recompense du parrain apparait.
     */
    public record MyReferralView(
            String referralNumber, String programName, String friend, ReferralStatus status,
            Instant registeredAt, Instant qualifiedAt, boolean rewardGranted, BigDecimal rewardAmount, String currency
    ) {}

    /** Ce que l'espace client montre d'un programme ouvert : ce que l'on gagne, ce que gagne l'ami. */
    public record OpenProgramView(
            String code, String name, String description, Instant validUntil,
            PromotionRef referrerPromotion, PromotionRef refereePromotion,
            ReferralQualificationRule qualificationRule, Integer qualificationDelayDays,
            Integer maxReferralsPerReferrer, String myLinkCode, String myLinkUrl
    ) {}

    /**
     * La page publique d'un lien, avant inscription. {@code valid} : le code existe, le lien est
     * actif et le programme ouvert ; sinon {@code reason} dit pourquoi. Le parrain n'y figure que
     * par son prenom.
     */
    public record PublicLinkView(
            String code, boolean valid, String reason, String programName, String programDescription,
            PromotionRef refereePromotion, String referrerFirstName
    ) {}
}
