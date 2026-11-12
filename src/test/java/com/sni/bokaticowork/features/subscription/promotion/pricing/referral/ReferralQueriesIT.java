package com.sni.bokaticowork.features.subscription.promotion.pricing.referral;

import com.sni.bokaticowork.features.subscription.promotion.model.Promotion;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.enums.ReferralProgramStatus;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.enums.ReferralStatus;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.model.Referral;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.model.ReferralLink;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.model.ReferralProgram;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.repository.ReferralLinkRepository;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.repository.ReferralProgramRepository;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.repository.ReferralRepository;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Les lectures du parrainage sur le schema de production : liste filtrable du back-office (chaque
 * filtre facultatif), parrainages et liens d'un parrain, compte par etat et liens d'un programme.
 */
class ReferralQueriesIT extends PostgresIntegrationTestBase {

    @Autowired private EntityManager entityManager;
    @Autowired private ReferralProgramRepository programs;
    @Autowired private ReferralLinkRepository links;
    @Autowired private ReferralRepository referrals;

    @Test
    @DisplayName("Liste filtrable, parrainages et liens d'un parrain, comptes par etat")
    void readQueries() {
        // La base exige une recompense par programme (ck_referral_program_rewards)
        entityManager.createNativeQuery("""
                INSERT INTO promotion (id, code, name, discount_type, discount_value, starts_at, status)
                VALUES (993000001, 'PROMO-IT-PARRAIN', 'Credit parrain', 'FIXED_AMOUNT', 5000, now(), 'ACTIVE')
                """).executeUpdate();
        Promotion reward = entityManager.getReference(Promotion.class, 993000001L);
        ReferralProgram program = programs.saveAndFlush(ReferralProgram.builder().referrerPromotion(reward)
                .code("AMI-IT").name("Parrainez un ami").status(ReferralProgramStatus.ACTIVE).currency("XAF").build());
        ReferralProgram other = programs.saveAndFlush(ReferralProgram.builder().refereePromotion(reward)
                .code("PRO-IT").name("Parrainage entreprises").status(ReferralProgramStatus.DRAFT).build());
        ReferralLink awa = links.saveAndFlush(ReferralLink.builder().program(program)
                .referrerType("MEMBER").referrerCode("MBR-AWA").code("AWAIT1").build());
        links.saveAndFlush(ReferralLink.builder().program(other)
                .referrerType("MEMBER").referrerCode("MBR-AWA").code("AWAIT2").build());
        referral(program, awa, "REF-IT-1", "MBR-F1", ReferralStatus.PENDING);
        referral(program, awa, "REF-IT-2", "MBR-F2", ReferralStatus.REWARDED);
        referral(program, awa, "REF-IT-3", "MBR-F3", ReferralStatus.REWARDED);
        entityManager.clear();

        assertThat(referrals.search(null, null, null, PageRequest.of(0, 20)).getTotalElements()).isEqualTo(3);
        assertThat(referrals.search(program.getId(), ReferralStatus.REWARDED, null, PageRequest.of(0, 20)).getContent())
                .extracting(Referral::getReferralNumber).containsExactlyInAnyOrder("REF-IT-2", "REF-IT-3");
        assertThat(referrals.search(null, null, "mbr-awa", PageRequest.of(0, 20)).getTotalElements()).isEqualTo(3);
        assertThat(referrals.search(other.getId(), null, null, PageRequest.of(0, 20)).getTotalElements()).isZero();

        assertThat(referrals.findAllForReferrer("member", "MBR-AWA")).hasSize(3);
        assertThat(links.findAllForReferrer("MEMBER", "mbr-awa")).extracting(ReferralLink::getCode)
                .containsExactlyInAnyOrder("AWAIT1", "AWAIT2");
        assertThat(links.countByProgram_Id(program.getId())).isEqualTo(1);
        assertThat(links.findByProgram_IdOrderByCreatedAtDesc(program.getId(), PageRequest.of(0, 10)).getContent()).hasSize(1);

        List<Object[]> byStatus = referrals.countByStatus(program.getId());
        assertThat(byStatus).extracting(row -> ((ReferralStatus) row[0]).name() + "=" + ((Number) row[1]).longValue())
                .containsExactlyInAnyOrder("PENDING=1", "REWARDED=2");
        assertThat(programs.findAllByOrderByCreatedAtDesc()).extracting(ReferralProgram::getCode).contains("AMI-IT", "PRO-IT");
    }

    private void referral(ReferralProgram program, ReferralLink link, String number, String referee, ReferralStatus status) {
        referrals.saveAndFlush(Referral.builder().referralNumber(number).program(program).link(link)
                .referrerType("MEMBER").referrerCode("MBR-AWA").refereeType("MEMBER").refereeCode(referee)
                .status(status).currency("XAF").build());
    }
}
