package com.sni.bokaticowork.features.company.sharing.service;

import com.sni.bokaticowork.features.billing.service.support.InvoiceUsageAnnex;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.sharing.model.SeatUsage;
import com.sni.bokaticowork.features.company.sharing.repository.SeatUsageRepository;
import com.sni.bokaticowork.features.subscription.overage.model.SubscriptionOverageCharge;
import com.sni.bokaticowork.features.subscription.overage.repository.SubscriptionOverageChargeRepository;
import com.sni.bokaticowork.features.subscription.repository.EntitlementDefinitionRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementUnit;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementDefinition;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.usage.model.UsageRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * L'annexe de la facture de renouvellement · ce que chaque collaborateur a pris sur la periode ecoulee.
 */
class SeatUsageAnnexProviderTest {

    private final SubscriptionRepository subscriptions = mock(SubscriptionRepository.class);
    private final SeatUsageRepository usages = mock(SeatUsageRepository.class);
    private final SubscriptionOverageChargeRepository charges = mock(SubscriptionOverageChargeRepository.class);
    private final MemberRepository members = mock(MemberRepository.class);
    private final EntitlementDefinitionRepository definitions = mock(EntitlementDefinitionRepository.class);
    private final SeatUsageAnnexProvider provider = new SeatUsageAnnexProvider(subscriptions, usages, charges, members, definitions);

    private static final LocalDate OCTOBER = LocalDate.of(2026, 10, 1);
    private static final LocalDate NOVEMBER = LocalDate.of(2026, 11, 1);

    @BeforeEach
    void setUp() {
        Subscription subscription = new Subscription();
        subscription.setId(10L);
        subscription.setSubscriptionNumber("SUB-ACME");
        subscription.setCurrency("XAF");
        when(subscriptions.findBySubscriptionNumber("SUB-ACME")).thenReturn(Optional.of(subscription));
        when(usages.findLastPeriodBefore(eq(10L), eq(NOVEMBER), any())).thenReturn(OCTOBER);
        Member jean = new Member();
        jean.setMemberId("MBR-1");
        jean.setFirstname("Jean");
        jean.setLastname("Mbemba");
        when(members.findByMemberIdAndDeletedFalse("MBR-1")).thenReturn(Optional.of(jean));
        when(definitions.findByCodeIgnoreCase("SALLE_H")).thenReturn(Optional.of(
                EntitlementDefinition.builder().code("SALLE_H").name("Salle de réunion").unit(EntitlementUnit.HOUR).build()));
    }

    private static BillableItem renewal(SubscriberType type) {
        return BillableItem.builder().sourceType("SUBSCRIPTION_RENEWAL").sourceId("SUB-ACME").subscriberType(type)
                .billingPeriodStart(NOVEMBER).build();
    }

    private static SeatUsage usage(String quota, String pool) {
        BigDecimal q = new BigDecimal(quota), p = new BigDecimal(pool);
        return SeatUsage.builder().subscriptionId(10L).memberCode("MBR-1").entitlementCode("SALLE_H").periodStart(OCTOBER)
                .fromQuota(q).fromPool(p).quantity(q.add(p)).status(SeatUsage.Status.CONSUMED).build();
    }

    @Test
    @DisplayName("Une ligne par collaborateur et par droit · part, pot, total et depassement pour memoire")
    void aggregatesPerMember() {
        when(usages.findConsumedInPeriod(10L, OCTOBER)).thenReturn(List.of(usage("3", "0"), usage("2", "4")));
        UsageRecord record = UsageRecord.builder().beneficiaryType("MEMBER").beneficiaryCode("MBR-1").build();
        when(charges.findBeneficiaryCharges(eq(10L), any(), any())).thenReturn(List.of(SubscriptionOverageCharge.builder()
                .usageRecord(record).entitlementCode("SALLE_H").overageQuantity(new BigDecimal("2"))
                .amount(new BigDecimal("10000")).build()));

        InvoiceUsageAnnex annex = provider.annexFor(List.of(renewal(SubscriberType.CUSTOMER)));

        assertThat(annex.periodStart()).isEqualTo(OCTOBER);
        assertThat(annex.periodEnd()).isEqualTo(LocalDate.of(2026, 10, 31));
        assertThat(annex.rows()).hasSize(1);
        InvoiceUsageAnnex.Row row = annex.rows().get(0);
        assertThat(row.memberName()).isEqualTo("Jean Mbemba");
        assertThat(row.entitlementName()).isEqualTo("Salle de réunion");
        assertThat(row.fromQuota()).isEqualByComparingTo("5");
        assertThat(row.fromPool()).isEqualByComparingTo("4");
        assertThat(row.consumed()).isEqualByComparingTo("9");
        assertThat(row.overage()).isEqualByComparingTo("2");
        assertThat(row.overageAmount()).isEqualByComparingTo("10000");
    }

    @Test
    @DisplayName("Pas d'annexe pour un membre, une souscription, ou une periode sans consommation")
    void noAnnexOtherwise() {
        when(usages.findConsumedInPeriod(10L, OCTOBER)).thenReturn(List.of(usage("1", "0")));
        assertThat(provider.annexFor(List.of(renewal(SubscriberType.MEMBER)))).isNull();
        assertThat(provider.annexFor(List.of(BillableItem.builder().sourceType("SUBSCRIPTION_RECURRING").sourceId("SUB-ACME")
                .subscriberType(SubscriberType.CUSTOMER).billingPeriodStart(NOVEMBER).build()))).isNull();

        when(usages.findLastPeriodBefore(eq(10L), eq(NOVEMBER), any())).thenReturn(null);
        assertThat(provider.annexFor(List.of(renewal(SubscriberType.CUSTOMER)))).isNull();
    }
}
