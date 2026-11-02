package com.sni.bokaticowork.features.company.wallet.service;

import com.sni.bokaticowork.features.company.wallet.model.CompanyWalletSpend;
import com.sni.bokaticowork.features.company.wallet.model.SpendingAuthorization;
import com.sni.bokaticowork.features.company.wallet.repository.CompanyWalletSpendRepository;
import com.sni.bokaticowork.features.company.wallet.repository.SpendingAuthorizationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Les essais d'un code se comptent, et finissent par bloquer.
 */
class CompanySpendCodeGuardTest {

    private static final Instant NOW = Instant.parse("2026-11-02T09:00:00Z");

    private final SpendingAuthorizationRepository authorizations = mock(SpendingAuthorizationRepository.class);
    private final CompanyWalletSpendRepository spends = mock(CompanyWalletSpendRepository.class);
    private final CompanySpendCodeGuard guard = new CompanySpendCodeGuard(authorizations, spends);

    @Test
    @DisplayName("Code de depense : bon, faux, puis bloque au dernier essai")
    void pinLocksAfterTooManyAttempts() {
        SpendingAuthorization authorization = SpendingAuthorization.builder().id(1L).pinHash(guard.hash("2580")).build();
        when(authorizations.findById(1L)).thenReturn(Optional.of(authorization));

        assertThat(guard.verifyPin(1L, "2580", 3, 15, NOW).verdict()).isEqualTo(CompanySpendCodeGuard.Verdict.OK);
        assertThat(guard.verifyPin(1L, "0000", 3, 15, NOW).remaining()).isEqualTo(2);
        assertThat(guard.verifyPin(1L, "0000", 3, 15, NOW).remaining()).isEqualTo(1);
        CompanySpendCodeGuard.Check locked = guard.verifyPin(1L, "0000", 3, 15, NOW);
        assertThat(locked.verdict()).isEqualTo(CompanySpendCodeGuard.Verdict.LOCKED);
        assertThat(locked.lockedUntil()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
        // Bloque, meme le bon code ne passe pas avant l'echeance.
        assertThat(guard.verifyPin(1L, "2580", 3, 15, NOW).verdict()).isEqualTo(CompanySpendCodeGuard.Verdict.LOCKED);
        assertThat(guard.verifyPin(1L, "2580", 3, 15, NOW.plus(Duration.ofMinutes(16))).verdict())
                .isEqualTo(CompanySpendCodeGuard.Verdict.OK);
    }

    @Test
    @DisplayName("Code a usage unique : trois essais, puis la depense expire ; echu, il ne sert plus")
    void otpExpiresAfterThreeAttemptsOrInTime() {
        CompanyWalletSpend spend = CompanyWalletSpend.builder().shortId(9L).status(CompanyWalletSpend.Status.AWAITING_OTP)
                .otpHash(guard.hash("123456")).otpExpiresAt(NOW.plus(Duration.ofMinutes(10))).otpAttempts(0).build();
        when(spends.lockByShortId(9L)).thenReturn(Optional.of(spend));

        assertThat(guard.verifyOtp(9L, "000000", NOW).remaining()).isEqualTo(2);
        assertThat(guard.verifyOtp(9L, "000000", NOW).remaining()).isEqualTo(1);
        assertThat(guard.verifyOtp(9L, "000000", NOW).verdict()).isEqualTo(CompanySpendCodeGuard.Verdict.EXPIRED);
        assertThat(spend.getStatus()).isEqualTo(CompanyWalletSpend.Status.EXPIRED);

        CompanyWalletSpend late = CompanyWalletSpend.builder().shortId(10L).status(CompanyWalletSpend.Status.AWAITING_OTP)
                .otpHash(guard.hash("123456")).otpExpiresAt(NOW.minusSeconds(1)).otpAttempts(0).build();
        when(spends.lockByShortId(10L)).thenReturn(Optional.of(late));
        assertThat(guard.verifyOtp(10L, "123456", NOW).verdict()).isEqualTo(CompanySpendCodeGuard.Verdict.EXPIRED);

        CompanyWalletSpend fresh = CompanyWalletSpend.builder().shortId(11L).status(CompanyWalletSpend.Status.AWAITING_OTP)
                .otpHash(guard.hash("123456")).otpExpiresAt(NOW.plus(Duration.ofMinutes(5))).otpAttempts(0).build();
        when(spends.lockByShortId(11L)).thenReturn(Optional.of(fresh));
        assertThat(guard.verifyOtp(11L, "123456", NOW).verdict()).isEqualTo(CompanySpendCodeGuard.Verdict.OK);
        assertThat(fresh.getOtpHash()).isNull();
    }
}
