package com.sni.bokaticowork.features.subscription.promotion.pricing.referral.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.dto.response.WalletResponse;
import com.sni.bokaticowork.features.payment.enums.WalletEntryType;
import com.sni.bokaticowork.features.payment.enums.WalletStatus;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletService;
import com.sni.bokaticowork.features.subscription.promotion.enums.DiscountType;
import com.sni.bokaticowork.features.subscription.promotion.model.Promotion;
import com.sni.bokaticowork.features.subscription.promotion.pricing.audience.service.PromotionBeneficiaryService;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.RewardType;
import com.sni.bokaticowork.features.subscription.promotion.pricing.model.PromotionReward;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.enums.ReferralProgramStatus;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.enums.ReferralQualificationRule;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.enums.ReferralStatus;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.model.Referral;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.model.ReferralLink;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.model.ReferralProgram;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.repository.ReferralLinkRepository;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.repository.ReferralProgramRepository;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.repository.ReferralRepository;
import com.sni.bokaticowork.features.subscription.promotion.pricing.repository.PromotionRewardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Le parrainage.
 *
 * <p>La règle protégée ici tient en une phrase : un parrainage se qualifie, il ne se déclare pas.
 * Récompenser dès l'inscription revient à payer pour des comptes créés et jamais utilisés, qui sont
 * précisément ce qu'attire un programme payant trop tôt.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReferralServiceTest {

    @Mock private ReferralProgramRepository programRepository;
    @Mock private ReferralLinkRepository linkRepository;
    @Mock private ReferralRepository referralRepository;
    @Mock private PromotionRewardRepository rewardRepository;
    @Mock private PromotionBeneficiaryService beneficiaryService;
    @Mock private WalletService walletService;
    @Mock private SequenceGeneratorFacade sequenceGenerator;

    @InjectMocks
    private ReferralService service;

    private ReferralProgram program;
    private ReferralLink link;

    @BeforeEach
    void setUp() {
        program = ReferralProgram.builder()
                .code("PARRAIN")
                .name("Programme de parrainage")
                .status(ReferralProgramStatus.ACTIVE)
                .qualificationRule(ReferralQualificationRule.FIRST_PAYMENT)
                .currency("XAF")
                .build();
        program.setId(1L);

        link = ReferralLink.builder()
                .program(program)
                .referrerType("MEMBER")
                .referrerCode("MEM-PARRAIN")
                .code("ABCD2345")
                .clickCount(0)
                .signupCount(0)
                .active(Boolean.TRUE)
                .build();
        link.setId(1L);

        when(programRepository.findByCode(anyString())).thenReturn(Optional.of(program));
        when(linkRepository.findByCode(anyString())).thenReturn(Optional.of(link));
        when(linkRepository.save(any())).thenAnswer(call -> call.getArgument(0));
        when(referralRepository.save(any())).thenAnswer(call -> call.getArgument(0));
        when(referralRepository.findByReferee(any(), anyString(), anyString())).thenReturn(Optional.empty());
        when(sequenceGenerator.next(anyString())).thenReturn("PAR-0001");
        when(rewardRepository.findAllByPromotionId(any())).thenReturn(List.of());
        when(walletService.getOrCreate(anyString(), anyString(), anyString())).thenReturn(wallet());
        when(walletService.serviceWallet(anyString())).thenReturn(new WalletAccount());
    }

    // -------------------------------------------------------------------------------------
    // Enregistrement
    // -------------------------------------------------------------------------------------

    @Test
    void registersWithoutGrantingAnything() {
        Referral referral = service.register("ABCD2345", "MEMBER", "MEM-FILLEUL");

        assertEquals(ReferralStatus.PENDING, referral.getStatus());
        assertFalse(referral.getRefereeRewardGranted());
        assertFalse(referral.getReferrerRewardGranted());
        verifyNoInteractions(walletService);
        assertEquals(1, link.getSignupCount());
    }

    /** C'est la première chose que quelqu'un essaie. */
    @Test
    void refusesSelfReferral() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.register("ABCD2345", "MEMBER", "MEM-PARRAIN"));

        assertTrue(ex.getMessage().contains("soi-même"));
    }

    @Test
    void refusesASecondReferralForTheSameReferee() {
        when(referralRepository.findByReferee(any(), anyString(), anyString()))
                .thenReturn(Optional.of(new Referral()));

        assertThrows(BadRequestException.class, () -> service.register("ABCD2345", "MEMBER", "MEM-FILLEUL"));
    }

    @Test
    void refusesAProgramThatIsNotOpen() {
        program.setStatus(ReferralProgramStatus.PAUSED);

        assertThrows(BadRequestException.class, () -> service.register("ABCD2345", "MEMBER", "MEM-FILLEUL"));
    }

    @Test
    void qualifiesImmediatelyWhenTheProgramPaysOnSignup() {
        program.setQualificationRule(ReferralQualificationRule.SIGNUP);
        program.setRefereePromotion(promotionWith(RewardType.WALLET_CREDIT, "5000"));

        Referral referral = service.register("ABCD2345", "MEMBER", "MEM-FILLEUL");

        assertEquals(ReferralStatus.REWARDED, referral.getStatus());
    }

    // -------------------------------------------------------------------------------------
    // Qualification et récompenses
    // -------------------------------------------------------------------------------------

    @Test
    void creditsTheWalletImmediatelyForAMonetaryReward() {
        program.setRefereePromotion(promotionWith(RewardType.WALLET_CREDIT, "5000"));
        Referral referral = pending();

        service.qualify(referral);

        verify(walletService).credit(any(), eq(new BigDecimal("5000")), eq(WalletEntryType.PROMOTIONAL_CREDIT),
                eq("REFERRAL"), anyString(), anyString(), anyString(), anyString());
        assertTrue(referral.getRefereeRewardGranted());
        assertEquals(new BigDecimal("5000"), referral.getRefereeRewardAmount());
    }

    /**
     * Toute récompense qui n'est pas de l'argent devient un droit nominatif sur la promotion
     * porteuse, que le moteur appliquera au prochain achat. C'est ce qui évite d'écrire un second
     * moteur pour dire la même chose.
     */
    @Test
    void turnsANonMonetaryRewardIntoANamedRightOnItsPromotion() {
        Promotion promotion = promotionWith(RewardType.PERCENTAGE_OFF, "10");
        program.setRefereePromotion(promotion);
        Referral referral = pending();

        service.qualify(referral);

        verify(beneficiaryService).add(eq(promotion.getCode()), eq("MEMBER"), eq("MEM-FILLEUL"),
                anyString(), anyString());
        verifyNoInteractions(walletService);
    }

    /**
     * Le plafond du parrain ne fait pas échouer la qualification : le filleul a rempli sa part et
     * garde son avantage. Refuser les deux punirait quelqu'un pour la popularité d'un autre.
     */
    @Test
    void stillRewardsTheRefereeWhenTheReferrerHasReachedTheirCap() {
        program.setMaxReferralsPerReferrer(3);
        program.setRefereePromotion(promotionWith(RewardType.WALLET_CREDIT, "5000"));
        program.setReferrerPromotion(promotionWith(RewardType.WALLET_CREDIT, "3000"));
        when(referralRepository.countAgainstCap(any(), anyString(), anyString())).thenReturn(9L);

        Referral referral = service.qualify(pending());

        assertTrue(referral.getRefereeRewardGranted());
        assertFalse(referral.getReferrerRewardGranted());
        assertEquals(ReferralStatus.REWARDED, referral.getStatus());
    }

    @Test
    void rewardsBothWhenTheReferrerIsWithinTheirCap() {
        program.setMaxReferralsPerReferrer(3);
        program.setRefereePromotion(promotionWith(RewardType.WALLET_CREDIT, "5000"));
        program.setReferrerPromotion(promotionWith(RewardType.WALLET_CREDIT, "3000"));
        when(referralRepository.countAgainstCap(any(), anyString(), anyString())).thenReturn(2L);

        Referral referral = service.qualify(pending());

        assertTrue(referral.getRefereeRewardGranted());
        assertTrue(referral.getReferrerRewardGranted());
    }

    @Test
    void doesNothingWhenQualifyingAnAlreadyQualifiedReferral() {
        Referral rewarded = pending();
        rewarded.setStatus(ReferralStatus.REWARDED);

        service.qualify(rewarded);

        verifyNoInteractions(walletService);
        verify(referralRepository, never()).save(any());
    }

    @Test
    void qualifiesOnlyTheProgramsThatPayOnFirstPayment() {
        ReferralProgram onSignup = ReferralProgram.builder()
                .code("AUTRE").status(ReferralProgramStatus.ACTIVE)
                .qualificationRule(ReferralQualificationRule.SIGNUP).build();
        onSignup.setId(2L);
        Referral other = pending();
        other.setProgram(onSignup);
        when(referralRepository.findPendingForReferee(anyString(), anyString()))
                .thenReturn(List.of(pending(), other));

        assertEquals(1, service.qualifyOnFirstPayment("MEMBER", "MEM-FILLEUL"));
    }

    @Test
    void refusesToRejectAnAlreadyRewardedReferral() {
        Referral rewarded = pending();
        rewarded.setStatus(ReferralStatus.REWARDED);
        when(referralRepository.findByNumber(anyString())).thenReturn(Optional.of(rewarded));

        assertThrows(BadRequestException.class, () -> service.reject("PAR-0001", "Abus"));
    }

    // -------------------------------------------------------------------------------------

    private Referral pending() {
        return Referral.builder()
                .referralNumber("PAR-0001")
                .program(program)
                .referrerType("MEMBER")
                .referrerCode("MEM-PARRAIN")
                .refereeType("MEMBER")
                .refereeCode("MEM-FILLEUL")
                .status(ReferralStatus.PENDING)
                .currency("XAF")
                .referrerRewardGranted(Boolean.FALSE)
                .refereeRewardGranted(Boolean.FALSE)
                .build();
    }

    private Promotion promotionWith(RewardType type, String value) {
        Promotion promotion = Promotion.builder()
                .code("PROMO-" + type.name())
                .name("Récompense " + type.name())
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.ZERO)
                .startsAt(Instant.now().minusSeconds(60))
                .build();
        promotion.setId((long) type.ordinal() + 100);
        when(rewardRepository.findAllByPromotionId(promotion.getId())).thenReturn(List.of(
                PromotionReward.builder().promotion(promotion).rewardType(type)
                        .value(new BigDecimal(value)).build()));
        return promotion;
    }

    private WalletResponse wallet() {
        return new WalletResponse("WAL-0001", "MEMBER", "MEM-1", "XAF", WalletStatus.ACTIVE,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null);
    }
}
