package com.sni.bokaticowork.features.subscription.promotion.pricing.referral;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.subscription.promotion.model.Promotion;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.dto.ReferralDtos.LinkView;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.dto.ReferralDtos.ProgramRequest;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.dto.ReferralDtos.ProgramView;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.dto.ReferralDtos.PublicLinkView;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.enums.ReferralProgramStatus;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.enums.ReferralQualificationRule;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.event.RefereePaidEvent;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.event.ReferralPaymentListener;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.model.ReferralLink;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.model.ReferralProgram;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.repository.ReferralLinkRepository;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.repository.ReferralProgramRepository;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.repository.ReferralRepository;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.service.ReferralProgramService;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.service.ReferralService;
import com.sni.bokaticowork.features.subscription.promotion.repository.PromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Les programmes de parrainage : creation, ouverture, regles figees une fois ouverts, lien du
 * membre et page publique d'un lien.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReferralProgramServiceTest {

    @Mock private ReferralProgramRepository programRepository;
    @Mock private ReferralLinkRepository linkRepository;
    @Mock private ReferralRepository referralRepository;
    @Mock private PromotionRepository promotionRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private ReferralService referralService;

    @InjectMocks private ReferralProgramService service;

    private final Promotion credit = Promotion.builder().id(1L).code("PROMO-PARRAIN").name("5 000 offerts").build();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "shareUrlTemplate", "https://portail.test/inscription?parrain={code}");
        when(programRepository.save(any(ReferralProgram.class))).thenAnswer(call -> call.getArgument(0));
        when(referralRepository.countByStatus(any())).thenReturn(List.of());
        when(promotionRepository.findByCodeIgnoreCase("PROMO-PARRAIN")).thenReturn(Optional.of(credit));
    }

    private static ProgramRequest request(String referrerPromotion, ReferralQualificationRule rule, Integer delay) {
        return new ProgramRequest("ami-2026", "Parrainez un ami", null, null, null,
                referrerPromotion, null, rule, delay, 5, null);
    }

    private ReferralProgram stored(ReferralProgramStatus status) {
        ReferralProgram program = ReferralProgram.builder().id(9L).code("AMI-2026").name("Parrainez un ami")
                .status(status).referrerPromotion(credit).qualificationRule(ReferralQualificationRule.FIRST_PAYMENT).build();
        when(programRepository.findByCode("AMI-2026")).thenReturn(Optional.of(program));
        return program;
    }

    @Test
    @DisplayName("Un programme nait en brouillon, son code en majuscules, au premier paiement par defaut")
    void createsADraft() {
        ProgramView view = service.create(request("PROMO-PARRAIN", null, null), "admin");

        assertThat(view.code()).isEqualTo("AMI-2026");
        assertThat(view.status()).isEqualTo(ReferralProgramStatus.DRAFT);
        assertThat(view.open()).isFalse();
        assertThat(view.qualificationRule()).isEqualTo(ReferralQualificationRule.FIRST_PAYMENT);
        assertThat(view.referrerPromotion().code()).isEqualTo("PROMO-PARRAIN");
        assertThat(view.currency()).isEqualTo("XAF");
        assertThat(view.referralsByStatus()).containsKeys("PENDING", "QUALIFIED", "REWARDED", "REJECTED");
    }

    @Test
    @DisplayName("Un code deja pris et une anciennete sans delai sont refuses")
    void refusesDuplicatesAndTenureWithoutDelay() {
        stored(ReferralProgramStatus.DRAFT);
        assertThatThrownBy(() -> service.create(request("PROMO-PARRAIN", null, null), "admin")).isInstanceOf(ConflictException.class);

        when(programRepository.findByCode("AMI-2026")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(request("PROMO-PARRAIN", ReferralQualificationRule.TENURE_REACHED, null), "admin"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("délai");
    }

    @Test
    @DisplayName("Un programme sans aucune recompense ne s'ouvre pas ; avec une, il s'ouvre, se met en pause, se clot")
    void activationNeedsAReward() {
        ReferralProgram program = stored(ReferralProgramStatus.DRAFT);
        program.setReferrerPromotion(null);
        assertThatThrownBy(() -> service.activate("AMI-2026")).isInstanceOf(BadRequestException.class);

        program.setReferrerPromotion(credit);
        assertThat(service.activate("AMI-2026").open()).isTrue();
        assertThat(service.pause("AMI-2026").status()).isEqualTo(ReferralProgramStatus.PAUSED);
        assertThat(service.end("AMI-2026").status()).isEqualTo(ReferralProgramStatus.ENDED);
        assertThatThrownBy(() -> service.activate("AMI-2026")).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Une fois ouvert, le nom change encore, mais plus la regle ni les recompenses")
    void rulesAreFrozenOnceOpen() {
        stored(ReferralProgramStatus.ACTIVE);

        ProgramRequest rename = new ProgramRequest("AMI-2026", "Parrainage de rentree", null, null, null,
                "PROMO-PARRAIN", null, ReferralQualificationRule.FIRST_PAYMENT, null, 10, null);
        assertThat(service.update("AMI-2026", rename).name()).isEqualTo("Parrainage de rentree");

        ProgramRequest changeRule = new ProgramRequest("AMI-2026", "x", null, null, null,
                "PROMO-PARRAIN", null, ReferralQualificationRule.SIGNUP, null, 10, null);
        assertThatThrownBy(() -> service.update("AMI-2026", changeRule))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("promis");
    }

    @Test
    @DisplayName("Le lien du membre n'existe que sur un programme ouvert, et porte l'adresse a partager")
    void memberLinkOnOpenProgramOnly() {
        ReferralProgram program = stored(ReferralProgramStatus.DRAFT);
        Member member = Member.builder().memberId("MBR-1").firstname("Awa").lastname("Ndoye").build();
        assertThatThrownBy(() -> service.myLink(member, "AMI-2026")).isInstanceOf(BadRequestException.class);

        program.setStatus(ReferralProgramStatus.ACTIVE);
        ReferralLink link = ReferralLink.builder().program(program).referrerType("MEMBER").referrerCode("MBR-1")
                .code("K7P2QX").clickCount(3).signupCount(1).active(true).build();
        when(referralService.linkFor("AMI-2026", "MEMBER", "MBR-1")).thenReturn(link);

        LinkView view = service.myLink(member, "AMI-2026");
        assertThat(view.url()).isEqualTo("https://portail.test/inscription?parrain=K7P2QX");
        assertThat(view.programOpen()).isTrue();
        assertThat(view.clickCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("La page publique dit si un code sert, et pourquoi sinon ; l'inscription refuse un code inutilisable")
    void publicPageAndSignupCheck() {
        when(linkRepository.findByCode("INCONNU")).thenReturn(Optional.empty());
        assertThat(service.publicLink("INCONNU").valid()).isFalse();
        assertThatThrownBy(() -> service.requireUsableCode("INCONNU")).isInstanceOf(BadRequestException.class);

        ReferralProgram program = ReferralProgram.builder().code("AMI-2026").name("Parrainez un ami")
                .status(ReferralProgramStatus.ACTIVE).validUntil(Instant.now().plus(10, ChronoUnit.DAYS)).build();
        ReferralLink link = ReferralLink.builder().program(program).referrerType("MEMBER").referrerCode("MBR-1")
                .code("K7P2QX").active(true).build();
        when(linkRepository.findByCode("K7P2QX")).thenReturn(Optional.of(link));
        when(memberRepository.findByMemberIdAndDeletedFalse("MBR-1"))
                .thenReturn(Optional.of(Member.builder().memberId("MBR-1").firstname("Awa").lastname("Ndoye").build()));

        PublicLinkView page = service.publicLink("K7P2QX");
        assertThat(page.valid()).isTrue();
        assertThat(page.referrerFirstName()).isEqualTo("Awa");
        assertThatCode(() -> service.requireUsableCode("K7P2QX")).doesNotThrowAnyException();

        link.setActive(false);
        assertThat(service.publicLink("K7P2QX").reason()).contains("plus actif");
    }

    @Test
    @DisplayName("Une qualification qui echoue apres un paiement est journalisee, jamais propagee au paiement")
    void listenerNeverBreaksAPayment() {
        ReferralService referrals = mock(ReferralService.class);
        when(referrals.qualifyOnFirstPayment(anyString(), anyString())).thenThrow(new IllegalStateException("portefeuille verrouille"));
        ReferralPaymentListener listener = new ReferralPaymentListener(referrals);

        assertThatCode(() -> listener.onRefereePaid(new RefereePaidEvent("MEMBER", "MBR-2", "TXN-1"))).doesNotThrowAnyException();
        assertThatCode(() -> listener.onRefereePaid(new RefereePaidEvent(null, null, "TXN-2"))).doesNotThrowAnyException();
    }
}
