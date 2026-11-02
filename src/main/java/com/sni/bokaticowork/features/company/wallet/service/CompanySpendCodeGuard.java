package com.sni.bokaticowork.features.company.wallet.service;

import com.sni.bokaticowork.features.company.wallet.model.CompanyWalletSpend;
import com.sni.bokaticowork.features.company.wallet.model.SpendingAuthorization;
import com.sni.bokaticowork.features.company.wallet.repository.CompanyWalletSpendRepository;
import com.sni.bokaticowork.features.company.wallet.repository.SpendingAuthorizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;

/**
 * Verifie le code de depense d'un collaborateur et le code a usage unique d'une depense.
 *
 * <p>Chaque verification s'ecrit dans sa propre transaction : un essai manque est compte meme si
 * l'appelant leve ensuite une erreur et annule la sienne. Sans cela, le compteur d'essais serait
 * efface a chaque refus, et un code se devinerait sans limite.</p>
 */
@Component
@RequiredArgsConstructor
public class CompanySpendCodeGuard {

    public enum Verdict { OK, NO_CODE, WRONG, LOCKED, EXPIRED }

    /** Le resultat et ce qui reste d'essais · pour le message de refus. */
    public record Check(Verdict verdict, int remaining, Instant lockedUntil) {
    }

    static final int OTP_MAX_ATTEMPTS = 3;

    private final SpendingAuthorizationRepository authorizationRepository;
    private final CompanyWalletSpendRepository spendRepository;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    public String hash(String code) {
        return encoder.encode(code);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Check verifyPin(Long authorizationId, String pin, int maxAttempts, long lockoutMinutes, Instant now) {
        SpendingAuthorization authorization = authorizationRepository.findById(authorizationId).orElseThrow();
        if (!StringUtils.hasText(authorization.getPinHash())) {
            return new Check(Verdict.NO_CODE, 0, null);
        }
        if (authorization.getPinLockedUntil() != null && authorization.getPinLockedUntil().isAfter(now)) {
            return new Check(Verdict.LOCKED, 0, authorization.getPinLockedUntil());
        }
        int failed = authorization.getPinFailedAttempts() == null ? 0 : authorization.getPinFailedAttempts();
        if (StringUtils.hasText(pin) && encoder.matches(pin, authorization.getPinHash())) {
            authorization.setPinFailedAttempts(0);
            authorization.setPinLockedUntil(null);
            authorizationRepository.save(authorization);
            return new Check(Verdict.OK, maxAttempts, null);
        }
        failed++;
        if (failed >= maxAttempts) {
            authorization.setPinFailedAttempts(0);
            authorization.setPinLockedUntil(now.plus(Duration.ofMinutes(lockoutMinutes)));
            authorizationRepository.save(authorization);
            return new Check(Verdict.LOCKED, 0, authorization.getPinLockedUntil());
        }
        authorization.setPinFailedAttempts(failed);
        authorizationRepository.save(authorization);
        return new Check(Verdict.WRONG, maxAttempts - failed, null);
    }

    /** Le code a usage unique · trois essais, puis la depense expire. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Check verifyOtp(Long spendShortId, String code, Instant now) {
        CompanyWalletSpend spend = spendRepository.lockByShortId(spendShortId).orElseThrow();
        if (spend.getStatus() != CompanyWalletSpend.Status.AWAITING_OTP || spend.getOtpHash() == null) {
            return new Check(Verdict.EXPIRED, 0, null);
        }
        if (spend.getOtpExpiresAt() == null || !spend.getOtpExpiresAt().isAfter(now)) {
            expire(spend, now, "Code de confirmation échu");
            return new Check(Verdict.EXPIRED, 0, null);
        }
        if (StringUtils.hasText(code) && encoder.matches(code.trim(), spend.getOtpHash())) {
            spend.setOtpHash(null);
            spendRepository.save(spend);
            return new Check(Verdict.OK, 0, null);
        }
        int attempts = (spend.getOtpAttempts() == null ? 0 : spend.getOtpAttempts()) + 1;
        spend.setOtpAttempts(attempts);
        if (attempts >= OTP_MAX_ATTEMPTS) {
            expire(spend, now, "Trop d'essais du code de confirmation");
            return new Check(Verdict.EXPIRED, 0, null);
        }
        spendRepository.save(spend);
        return new Check(Verdict.WRONG, OTP_MAX_ATTEMPTS - attempts, null);
    }

    private void expire(CompanyWalletSpend spend, Instant now, String reason) {
        spend.setStatus(CompanyWalletSpend.Status.EXPIRED);
        spend.setOtpHash(null);
        spend.setDecidedBy("SYSTEM");
        spend.setDecidedAt(now);
        spend.setDecisionReason(reason);
        spendRepository.save(spend);
    }
}
