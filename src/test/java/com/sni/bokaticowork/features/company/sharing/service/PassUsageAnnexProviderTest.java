package com.sni.bokaticowork.features.company.sharing.service;

import com.sni.bokaticowork.features.billing.service.support.InvoiceUsageAnnex;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.subscription.repository.BillableItemRepository;
import com.sni.bokaticowork.features.subscription.repository.EntitlementDefinitionRepository;
import com.sni.bokaticowork.features.subscription.repository.EntitlementLedgerRepository;
import com.sni.bokaticowork.features.subscription.repository.PassRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * L'annexe du renouvellement d'un pass d'organisation · le total pris par chaque beneficiaire.
 */
class PassUsageAnnexProviderTest {

    private static final ZoneId ZONE = ZoneId.of("Africa/Brazzaville");
    private static final LocalDate OCTOBER = LocalDate.of(2026, 10, 1);
    private static final LocalDate NOVEMBER = LocalDate.of(2026, 11, 1);

    private final PassRepository passes = mock(PassRepository.class);
    private final BillableItemRepository items = mock(BillableItemRepository.class);
    private final EntitlementLedgerRepository ledger = mock(EntitlementLedgerRepository.class);
    private final MemberRepository members = mock(MemberRepository.class);
    private final PassUsageAnnexProvider provider = new PassUsageAnnexProvider(passes, items, ledger, members,
            mock(EntitlementDefinitionRepository.class), Clock.fixed(Instant.parse("2026-11-01T06:00:00Z"), ZONE));

    @BeforeEach
    void setUp() {
        Pass pass = new Pass();
        pass.setId(30L);
        pass.setPassNumber("PAS-ACME");
        pass.setCurrency("XAF");
        when(passes.findByPassNumber("PAS-ACME")).thenReturn(Optional.of(pass));
        Member jean = new Member();
        jean.setMemberId("MBR-1");
        jean.setFirstname("Jean");
        jean.setLastname("Mbemba");
        when(members.findByMemberIdAndDeletedFalse("MBR-1")).thenReturn(Optional.of(jean));
    }

    private static BillableItem renewal(SubscriberType owner) {
        return BillableItem.builder().sourceType("PASS_RENEWAL").sourceId("PAS-ACME").subscriberType(owner)
                .billingPeriodStart(NOVEMBER).build();
    }

    @Test
    @DisplayName("La periode ecoulee se lit sur la facturation precedente · une ligne par beneficiaire et par droit")
    void totalsPerBeneficiary() {
        when(items.findPreviousPassPeriodStart("PAS-ACME", NOVEMBER)).thenReturn(OCTOBER);
        List<Object[]> rows = List.<Object[]>of(new Object[]{"MEMBER", "MBR-1", "SALLE_H", new BigDecimal("6")});
        when(ledger.sumConsumedByBeneficiaryOnPassBetween(eq(30L), eq(OCTOBER.atStartOfDay(ZONE).toInstant()), any()))
                .thenReturn(rows);

        InvoiceUsageAnnex annex = provider.annexFor(List.of(renewal(SubscriberType.CUSTOMER)));

        assertThat(annex.pass()).isTrue();
        assertThat(annex.subscriptionNumber()).isEqualTo("PAS-ACME");
        assertThat(annex.periodEnd()).isEqualTo(LocalDate.of(2026, 10, 31));
        assertThat(annex.rows()).singleElement().satisfies(row -> {
            assertThat(row.memberName()).isEqualTo("Jean Mbemba");
            assertThat(row.consumed()).isEqualByComparingTo("6");
        });
    }

    @Test
    @DisplayName("Pas d'annexe pour le pass d'un membre, ni sans facturation precedente ni consommation")
    void noAnnexOtherwise() {
        assertThat(provider.annexFor(List.of(renewal(SubscriberType.MEMBER)))).isNull();
        when(items.findPreviousPassPeriodStart("PAS-ACME", NOVEMBER)).thenReturn(null);
        assertThat(provider.annexFor(List.of(renewal(SubscriberType.CUSTOMER)))).isNull();
        when(items.findPreviousPassPeriodStart("PAS-ACME", NOVEMBER)).thenReturn(OCTOBER);
        when(ledger.sumConsumedByBeneficiaryOnPassBetween(eq(30L), any(), any())).thenReturn(List.of());
        assertThat(provider.annexFor(List.of(renewal(SubscriberType.CUSTOMER)))).isNull();
    }
}
