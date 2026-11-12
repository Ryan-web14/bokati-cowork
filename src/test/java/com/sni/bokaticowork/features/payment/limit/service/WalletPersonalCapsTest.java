package com.sni.bokaticowork.features.payment.limit.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.service.support.MemberBillingContact;
import com.sni.bokaticowork.features.payment.control.service.WalletRiskFlagService;
import com.sni.bokaticowork.features.payment.limit.repository.WalletLimitPolicyRepository;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.repository.WalletLedgerEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Les plafonds personnels de depense que le membre s'est fixes.
 *
 * <p>Ils s'ajoutent aux limites du palier ; une depense qui ferait depasser le cumul du jour ou
 * des sept derniers jours est refusee en francais clair.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WalletPersonalCapsTest {

    @Mock private WalletLimitPolicyRepository policyRepository;
    @Mock private WalletLedgerEntryRepository ledgerRepository;
    @Mock private WalletRiskFlagService flagService;
    @Mock private MemberBillingContact memberContact;

    private WalletLimitService limitService;
    private WalletAccount wallet;

    @BeforeEach
    void setUp() {
        limitService = new WalletLimitService(policyRepository, ledgerRepository, flagService);
        limitService.setMemberContact(memberContact);
        wallet = WalletAccount.builder().id(1L).walletNumber("WAL-1").ownerType("MEMBER").ownerCode("MBR-1")
                .currency("XAF").availableBalance(new BigDecimal("100000")).ledgerBalance(new BigDecimal("100000"))
                .heldBalance(BigDecimal.ZERO).build();
        when(ledgerRepository.sumSince(anyLong(), anyString(), any(Instant.class))).thenReturn(BigDecimal.ZERO);
    }

    private void caps(Long daily, Long weekly) {
        when(memberContact.spendingCaps("MEMBER", "MBR-1"))
                .thenReturn(new MemberBillingContact.SpendingCaps(daily, weekly, ZoneId.of("Africa/Brazzaville")));
    }

    @Test
    @DisplayName("Sans plafond personnel, rien n'est refuse")
    void noCap() {
        when(memberContact.spendingCaps("MEMBER", "MBR-1")).thenReturn(MemberBillingContact.SpendingCaps.NONE);
        assertThatCode(() -> limitService.assertWithinPersonalCaps(wallet, new BigDecimal("999999"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Plafond journalier : le cumul du jour plus la depense ne doit pas le depasser")
    void dailyCap() {
        caps(20000L, null);
        when(ledgerRepository.sumSince(eq(1L), eq("PAYMENT"), any(Instant.class))).thenReturn(new BigDecimal("15000"));

        assertThatCode(() -> limitService.assertWithinPersonalCaps(wallet, new BigDecimal("5000"))).doesNotThrowAnyException();
        assertThatThrownBy(() -> limitService.assertWithinPersonalCaps(wallet, new BigDecimal("5001")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Plafond personnel journalier atteint");
    }

    @Test
    @DisplayName("Plafond sur 7 jours : transferts et frais comptent aussi")
    void weeklyCap() {
        caps(null, 50000L);
        when(ledgerRepository.sumSince(eq(1L), eq("TRANSFER_OUT"), any(Instant.class))).thenReturn(new BigDecimal("40000"));
        when(ledgerRepository.sumSince(eq(1L), eq("TRANSFER_FEE"), any(Instant.class))).thenReturn(new BigDecimal("500"));

        assertThatThrownBy(() -> limitService.assertWithinPersonalCaps(wallet, new BigDecimal("10000")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Plafond personnel hebdomadaire atteint");
        assertThatCode(() -> limitService.assertWithinPersonalCaps(wallet, new BigDecimal("9500"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Sans preferences branchees, le controle est neutre")
    void withoutContact() {
        WalletLimitService bare = new WalletLimitService(policyRepository, ledgerRepository, flagService);
        assertThatCode(() -> bare.assertWithinPersonalCaps(wallet, new BigDecimal("1"))).doesNotThrowAnyException();
        assertThat(wallet.getWalletNumber()).isEqualTo("WAL-1");
    }
}
